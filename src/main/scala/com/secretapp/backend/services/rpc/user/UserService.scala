package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.{ ApiBrokerService, UpdatesBroker }
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.rpc._
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

  private def scaleAvatar(fl: FileLocation): Future[Avatar] = {
    val fr = new FileRecord

    for (
      fullImageBytes   <- fr.getFile(fl.fileId.toInt);
      (fiw, fih)       <- AvatarUtils.dimensions(fullImageBytes);

      smallImageId     <- ask(clusterProxies.filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];
      largeImageId     <- ask(clusterProxies.filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];

      _                <- fr.createFile(smallImageId, (new Random).nextString(30)); // TODO: genAccessSalt makes specs
      _                <- fr.createFile(largeImageId, (new Random).nextString(30)); // fail

      smallImageHash   <- fr.getAccessHash(smallImageId);
      largeImageHash   <- fr.getAccessHash(largeImageId);

      smallImageLoc    = FileLocation(smallImageId, smallImageHash);
      largeImageLoc    = FileLocation(largeImageId, largeImageHash);

      smallImageBytes <- AvatarUtils.resizeToSmall(fullImageBytes);
      largeImageBytes <- AvatarUtils.resizeToLarge(fullImageBytes);

      _               <- fr.write(smallImageLoc.fileId.toInt, 0, smallImageBytes);
      _               <- fr.write(largeImageLoc.fileId.toInt, 0, largeImageBytes);

      smallAvatarImage = AvatarImage(smallImageLoc, 100, 100, smallImageBytes.length);
      largeAvatarImage = AvatarImage(largeImageLoc, 200, 200, largeImageBytes.length);
      fullAvatarImage  = AvatarImage(fl, fiw, fih, fullImageBytes.length);

      avatar           = Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some)

    ) yield avatar
  }

  private def handleEditAvatar(user: User, r: RequestEditAvatar): Future[RpcResponse] = {
    val sizeLimit: Long = 1024 * 1024
    val fr = new FileRecord

    fr.getFileLength(r.fileLocation.fileId.toInt) flatMap { len =>
      if (len > sizeLimit)
        Future successful Error(400, "FILE_TOO_BIG", "", false)
      else
        scaleAvatar(r.fileLocation) flatMap { a =>
          UserRecord.updateAvatar(user.uid, a) map { _ =>
            withRelatedAuthIds(user.uid) { authIds =>
              authIds foreach { authId =>
                updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(authId, AvatarChanged(user.uid, Some(a)))
              }
            }
            Ok(ResponseAvatarChanged(a))
          }
        } recover {
          case e =>
            log.warning(s"Failed while updating avatar: $e")
            Error(400, "IMAGE_LOAD_ERROR", "", false)
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
