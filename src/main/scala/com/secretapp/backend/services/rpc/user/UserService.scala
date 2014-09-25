package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Ok, RpcRequestMessage, RpcResponse}
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{RequestUpdateUser, RequestSetAvatar, ResponseAvatarUploaded}
import com.secretapp.backend.data.message.struct.{Avatar, AvatarImage}
import com.secretapp.backend.data.message.update.AvatarChanged
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.{FileRecord, UserRecord}
import com.sksamuel.scrimage.{AsyncImage, Format, Position}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scalaz._
import Scalaz._

object AvatarUtils {

  private def resizeTo(imgBytes: Array[Byte], side: Int)
                      (implicit ec: ExecutionContext): Future[Array[Byte]] =
    for (
      img          <- AsyncImage(imgBytes);
      scaleCoef     = side.toDouble / math.min(img.width, img.height);
      scaledImg    <- img.scale(scaleCoef);
      resizedImg   <- scaledImg.resizeTo(side, side, Position.Center);
      resizedBytes <- resizedImg.writer(Format.JPEG).write()
    ) yield resizedBytes

  def resizeToSmall(imgBytes: Array[Byte])
                   (implicit ec: ExecutionContext) =
    resizeTo(imgBytes, 100)

  def resizeToLarge(imgBytes: Array[Byte])
                   (implicit ec: ExecutionContext) =
    resizeTo(imgBytes, 200)

  def dimensions(imgBytes: Array[Byte])
                (implicit ec: ExecutionContext): Future[(Int, Int)] =
    AsyncImage(imgBytes) map { i => (i.width, i.height) }

}

trait UserService {
  self: ApiBrokerService =>

  import context._

  def handleRpcUser: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestSetAvatar => authorizedRequest {
      // FIXME: don't use Option.get
      handleSetAvatar(currentUser.get, r)
    }

    case r: RequestUpdateUser => authorizedRequest {
      handleUpdateUser(currentUser.get, r)
    }
  }

  private def handleSetAvatar(user: User, r: RequestSetAvatar): Future[RpcResponse] = {
    val fr = new FileRecord

    val avatar = for (
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

      avatar           = Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some)

    ) yield avatar

    avatar flatMap { a =>
      UserRecord.updateAvatar(user.uid, a) map { _ =>
        sendAvatarChangedUpdates(user, a)
        Ok(ResponseAvatarUploaded(a))
      }
    }
  }

  private def handleUpdateUser(user: User, r: RequestUpdateUser): Future[RpcResponse] = {
    UserRecord.updateName(user.uid, r.name) map { _ =>
      Ok(ResponseVoid())
    }
  }

  private def sendAvatarChangedUpdates(u: User, avatar: Avatar): Unit = withRelations(u.uid) {
    _.foreach(pushUpdate(_, AvatarChanged(u.uid, avatar.some)))
  }
}
