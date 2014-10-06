package com.secretapp.backend.services.rpc.typing

import akka.actor._
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.{ RpcResponseBox, UpdateBox }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.{ResponseVoid, Error, Ok, RpcResponse}
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.helpers.UserHelpers
import com.secretapp.backend.session.SessionProtocol
import scala.collection.immutable
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait HandlerService extends UserHelpers {
  this: Handler =>

  import context.system
  import context.dispatcher
  import TypingProtocol._

  protected def handleRequestTyping(uid: Int, accessHash: Long, typingType: Int): Future[RpcResponse] = {
    log.info(s"handling RequestTyping $uid, $accessHash, $typingType")
    getUsers(uid) map {
      case users if users.isEmpty =>
        Error(404, "USER_DOES_NOT_EXISTS", "User does not exists.", true)
      case users =>
        val (_, checkUser) = users.head

        if (checkUser.accessHash(currentUser.authId) != accessHash) {
          Error(401, "ACCESS_HASH_INVALID", "Invalid user access hash.", false)
        } else {
          typingBrokerRegion ! Envelope(uid, UserTyping(currentUser.uid, typingType))
          Ok(ResponseVoid())
        }
    }
  }
}
