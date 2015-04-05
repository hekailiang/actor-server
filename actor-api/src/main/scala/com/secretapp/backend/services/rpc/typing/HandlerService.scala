package com.secretapp.backend.services.rpc.typing

import akka.actor._
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Error, Ok, RpcResponse}
import com.secretapp.backend.persist
import com.secretapp.backend.helpers.UserHelpers
import com.secretapp.backend.util.ACL
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait HandlerService extends UserHelpers {
  this: Handler =>

  import context.system
  import context.dispatcher
  import TypingProtocol._

  protected def handleRequestTyping(userId: Int, accessHash: Long, typingType: Int): Future[RpcResponse] = {
    getUserDatas(userId) map {
      case users if users.isEmpty =>
        Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true)
      case users =>
        val (_, checkUser) = users.head

        if (ACL.userAccessHash(currentUser.authId, checkUser.id, checkUser.accessSalt) != accessHash) {
          Error(401, "ACCESS_HASH_INVALID", "Invalid user access hash.", false)
        } else {
          typingBrokerRegion ! Envelope(userId, UserTyping(currentUser.uid, typingType))
          Ok(ResponseVoid())
        }
    }
  }

  protected def handleRequestGroupTyping(groupId: Int, accessHash: Long, typingType: Int): Future[RpcResponse] = {
    persist.Group.find(groupId) map {
      case Some(group) =>
        if (group.accessHash != accessHash) {
          Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false)
        } else {
          typingBrokerRegion ! GroupEnvelope(groupId, UserTyping(currentUser.uid, typingType))
          Ok(ResponseVoid())
        }
      case None =>
        Error(404, "GROUP_DOES_NOT_EXISTS", "Group does not exists.", true)
    }
  }
}
