package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.{ ApiBrokerService, UpdatesBroker }
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Ok, RpcRequestMessage, RpcResponse}
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{RequestEditName, RequestEditAvatar, ResponseAvatarChanged}
import com.secretapp.backend.data.message.struct.{Avatar, AvatarImage}
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.helpers.{SocialHelpers, UserHelpers}
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

trait UserService extends SocialHelpers with UserHelpers {
  self: ApiBrokerService =>

  import context._

  def handleRpcUser: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestEditAvatar => authorizedRequest {
      // FIXME: don't use Option.get
      handleEditAvatar(currentUser.get, r)
    }
    case r: RequestEditName => authorizedRequest {
      handleEditName(currentUser.get, r)
    }
  }

  private def handleEditAvatar(user: User, r: RequestEditAvatar): Future[RpcResponse] = {
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
        withRelatedAuthIds(user.uid) { authIds =>
          authIds foreach { authId =>
            updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(authId, AvatarChanged(user.uid, Some(a)))
          }
        }
        Ok(ResponseAvatarChanged(a))
      }
    }
  }

  private def handleEditName(user: User, r: RequestEditName): Future[RpcResponse] =
    UserRecord.updateName(user.uid, r.name) map { _ =>
      withRelatedAuthIds(user.uid) { authIds =>
        authIds foreach { authId =>
          updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(authId, NameChanged(user.uid, Some(r.name)))
        }
      }

      Ok(ResponseVoid())
    }
}
