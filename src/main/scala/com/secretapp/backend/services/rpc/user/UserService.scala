package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcRequestMessage, RpcResponse }
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{RequestSetAvatar, ResponseAvatarUploaded}
import com.secretapp.backend.data.message.struct.{Avatar, AvatarImage}
import com.secretapp.backend.data.message.update.AvatarChanged
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.{FileRecord, UserRecord}
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.rpc.files.FilesService
import com.sksamuel.scrimage.{AsyncImage, Format, Position}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scalaz.Scalaz._
import scalaz._

object AvatarUtils {

  private def resizeTo(imgBytes: Array[Byte], w: Int, h: Int)
              (implicit ec: ExecutionContext): Future[Array[Byte]] =
    for (
      img          <- AsyncImage(imgBytes);
      resizedImg   <- img.resizeTo(w, h, Position.Center);
      resizedBytes <- resizedImg.writer(Format.JPEG).write()
    ) yield resizedBytes

  def resizeToSmall(imgBytes: Array[Byte])
                   (implicit ec: ExecutionContext) =
    resizeTo(imgBytes, 100, 100)

  def resizeToLarge(imgBytes: Array[Byte])
                   (implicit ec: ExecutionContext) =
    resizeTo(imgBytes, 200, 200)

  def dimensions(imgBytes: Array[Byte])
                (implicit ec: ExecutionContext): Future[(Int, Int)] =
    AsyncImage(imgBytes) map { i => (i.width, i.height) }

}

trait UserService {
  self: ApiBrokerService =>

  import context._

  def handleRpcUser: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestSetAvatar =>
      authorizedRequest {
        // FIXME: don't use Option.get
        handleSetAvatar(currentUser.get, r)
      }
  }

  private def handleSetAvatar(u: User, r: RequestSetAvatar): Future[RpcResponse] = {
    val fr = new FileRecord

    for (
      fullImageBytes  <- fr.getFile(r.fileLocation.fileId.toInt);
      (fiw, fih)      <- AvatarUtils.dimensions(fullImageBytes);

      smallImageId    <- ask(clusterProxies.filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];
      largeImageId    <- ask(clusterProxies.filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];

      _               <- fr.createFile(smallImageId, (new Random).nextString(30)); // TODO: genAccessSalt makes specs
      _               <- fr.createFile(largeImageId, (new Random).nextString(30)); // fail

      smallImageHash  <- fr.getAccessHash(smallImageId);
      largeImageHash  <- fr.getAccessHash(largeImageId);

      smallImageLoc    = FileLocation(smallImageId, smallImageHash);
      largeImageLoc    = FileLocation(largeImageId, largeImageHash);

      smallImageBytes <- AvatarUtils.resizeToSmall(fullImageBytes);
      largeImageBytes <- AvatarUtils.resizeToLarge(fullImageBytes);

      _               <- fr.write(smallImageLoc.fileId.toInt, 0, smallImageBytes);
      _               <- fr.write(largeImageLoc.fileId.toInt, 0, largeImageBytes);

      smallAvatarImage = AvatarImage(smallImageLoc, 100, 100, smallImageBytes.length);
      largeAvatarImage = AvatarImage(largeImageLoc, 200, 200, largeImageBytes.length);
      fullAvatarImage  = AvatarImage(r.fileLocation, fiw, fih, fullImageBytes.length);

      avatar           = Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some);

      _               <- UserRecord.updateAvatar(u.authId, u.uid, avatar)

    ) yield {
      sendUpdates(u, avatar)
      Ok(ResponseAvatarUploaded(avatar))
    }
  }

  private def sendUpdates(u: User, avatar: Avatar): Unit = withRelations(u.uid) {
    _.foreach(pushUpdate(_, AvatarChanged(u.uid, avatar.some)))
  }
}
