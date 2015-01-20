package com.secretapp.backend.api

import akka.actor._
import com.secretapp.backend.api.rpc._
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ Ok, Error, RpcRequest }
import com.secretapp.backend.models.User
import com.secretapp.backend.data.transport.{ MessageBox, MTPackage }
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.session.SessionProtocol
import im.actor.server.persist.file.adapter.FileAdapter
import scala.util.{ Failure, Success }
import scalaz._
import Scalaz._

object ApiBrokerProtocol {
  sealed trait ApiBrokerMessage

  case class ApiBrokerRequest(connector: ActorRef, messageId: Long, body: RpcRequest) extends ApiBrokerMessage
  case class AuthorizeUser(user: User) extends ApiBrokerMessage
}

class ApiBrokerActor(
  val currentAuthId: Long,
  val currentSessionId: Long,
  val singletons: Singletons,
  val fileAdapter: FileAdapter,
  val subscribedToUpdates: Boolean
) extends Actor with ActorLogging with ApiBrokerService {
  import ApiBrokerProtocol._

  import context._

  val sessionActor = context.parent

  def receive = {
    case AuthorizeUser(user) =>
      currentUser = Some(user)
    case msg @ ApiBrokerRequest(connector, messageId, body) =>
      val replyTo = sender()

      handleRpc(messageId)(body) match {
        case \/-(fresp) =>
          fresp onComplete {
            case Success(resp) =>
              replyTo.tell(
                SessionProtocol.SendRpcResponseBox(connector, RpcResponseBox(messageId, resp)),
                self)
            case Failure(error) =>
              replyTo.tell(
                SessionProtocol.SendRpcResponseBox(
                  connector,
                  RpcResponseBox(messageId, Error(500, "INTERNAL_SERVER_ERROR", error.getMessage, true))),
                self)
              log.error(error, s"Failed to handle rpc(right) $connector $messageId $body")
          }

        case -\/(UserNotAuthenticated) =>
          replyTo.tell(
            SessionProtocol.SendRpcResponseBox(
              connector, RpcResponseBox(messageId, Error(401, "USER_NOT_AUTHORIZED", "", true))),
            self)
        case -\/(error) =>
          replyTo.tell(
            SessionProtocol.SendRpcResponseBox(
              connector, RpcResponseBox(messageId, Error(500, "INTERNAL_SERVER_ERROR", error.getMessage, true))),
            self)
          log.error(error, s"Failed to handle rpc(left) $connector $messageId $body")
      }
  }
}
