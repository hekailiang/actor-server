package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.{ ApiBrokerService, UpdatesBroker }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.data.message.rpc.user._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.helpers.{ SocialHelpers, UpdatesHelpers, UserHelpers }
import com.secretapp.backend.persist
import com.secretapp.backend.util.AvatarUtils
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait UserService extends SocialHelpers with UserHelpers with UpdatesHelpers {
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
    case r: RequestRemoveAvatar => authorizedRequest {
      handleRemoveAvatar(currentUser.get)
    }
    case r: RequestChangePhoneTitle => authorizedRequest {
      handleChangePhoneTitle(currentUser.get, r)
    }
  }

  private def handleEditAvatar(user: models.User, r: RequestEditAvatar): Future[RpcResponse] = {
    val sizeLimit: Long = 1024 * 1024 // TODO: configurable

    persist.FileData.find(r.fileLocation.fileId) flatMap {
      case Some(models.FileData(_, _, len, _)) =>
        if (len > sizeLimit)
          Future successful Error(400, "FILE_TOO_BIG", "", false)
        else
          AvatarUtils.scaleAvatar(fileAdapter, r.fileLocation) flatMap { a =>
            persist.AvatarData.save[models.User](user.uid, a.avatarData) flatMap { _ =>
              withRelatedAuthIds(user.uid) { authIds =>
                authIds foreach { authId =>
                  updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(authId, UserAvatarChanged(user.uid, Some(a)))
                }
              }

              broadcastCUUpdateAndGetState(currentUser.get, UserAvatarChanged(user.uid, Some(a))) map {
                case (seq, state) =>
                  Ok(ResponseEditAvatar(a, seq, Some(state)))
              }
            }
          } recover {
            case e =>
              log.warning(s"Failed while updating avatar: $e")
              Error(400, "IMAGE_LOAD_ERROR", "", false)
          }
      case None =>
        Future successful Error(400, "LOCATION_INVALID", "", false)
    }
  }

  private def handleRemoveAvatar(user: models.User): Future[RpcResponse] = {
    persist.AvatarData.save[models.User](user.uid, models.AvatarData.empty) flatMap { _ =>
      broadcastCUUpdateAndGetState(currentUser.get, UserAvatarChanged(user.uid, None)) map {
        case (seq, state) =>
          Ok(ResponseSeq(seq, Some(state)))
      }
    }
  }

  private def handleEditName(user: models.User, r: RequestEditName): Future[RpcResponse] =
    persist.User.updateName(user.uid, r.name) flatMap { _ =>
      val update = NameChanged(user.uid, r.name)

      withRelatedAuthIds(user.uid) { authIds =>
        authIds foreach { authId =>
          if (authId != currentUser.get.authId)
            writeNewUpdate(authId, update)
        }
      }

      broadcastCUUpdateAndGetState(currentUser.get, update) map {
        case (seq, state) => Ok(ResponseSeq(seq, Some(state)))
      }
    }

  def handleChangePhoneTitle(user: models.User, r: RequestChangePhoneTitle): Future[RpcResponse] =
    persist.UserPhone.updateTitle(userId = user.uid, id = r.phoneId, title = r.title) flatMap { _ =>
      val update = PhoneTitleChanged(r.phoneId, r.title)

      withRelatedAuthIds(user.uid) { authIds =>
        authIds foreach { authId =>
          if (authId != currentUser.get.authId)
            writeNewUpdate(authId, update)
        }
      }

      broadcastCUUpdateAndGetState(user, update) map {
        case (seq, state) => Ok(ResponseSeq(seq, Some(state)))
      }
    }
}
