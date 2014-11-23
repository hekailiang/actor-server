package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.{ ApiBrokerService, UpdatesBroker }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.ResponseAvatarChanged
import com.secretapp.backend.data.message.rpc.update.ResponseSeq
import com.secretapp.backend.data.message.rpc.user.{ RequestEditName, RequestEditAvatar }
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
  }

  private def handleEditAvatar(user: models.User, r: RequestEditAvatar): Future[RpcResponse] = {
    val sizeLimit: Long = 1024 * 1024 // TODO: configurable

    fileRecord.getFileLength(r.fileLocation.fileId.toInt) flatMap { len =>
      if (len > sizeLimit)
        Future successful Error(400, "FILE_TOO_BIG", "", false)
      else
        AvatarUtils.scaleAvatar(fileRecord, r.fileLocation) flatMap { a =>
          persist.User.updateAvatar(user.uid, a) flatMap { _ =>
            withRelatedAuthIds(user.uid) { authIds =>
              authIds foreach { authId =>
                updatesBrokerRegion ! UpdatesBroker.NewUpdatePush(authId, AvatarChanged(user.uid, Some(a)))
              }
            }

            for {
              updateState <- ask(
                updatesBrokerRegion,
                UpdatesBroker.NewUpdatePush(user.authId, AvatarChanged(user.uid, Some(a)))
              ).mapTo[UpdatesBroker.StrictState]
            } yield {
              val (seq, state) = updateState
              Ok(ResponseAvatarChanged(a, seq, Some(state)))
            }
          }
        } recover {
          case e =>
            log.warning(s"Failed while updating avatar: $e")
            Error(400, "IMAGE_LOAD_ERROR", "", false)
        }
    }
  }

  private def handleEditName(user: models.User, r: RequestEditName): Future[RpcResponse] =
    persist.User.updateName(user.uid, r.name) flatMap { _ =>
      val update = NameChanged(user.uid, Some(r.name))

      withRelatedAuthIds(user.uid) { authIds =>
        authIds foreach { authId =>
          if (authId != currentUser.get.authId)
            writeNewUpdate(authId, update)
        }
      }

      withNewUpdateState(currentUser.get.authId, update) {
        case (seq, state) => Ok(ResponseSeq(seq, Some(state)))
      }
    }
}
