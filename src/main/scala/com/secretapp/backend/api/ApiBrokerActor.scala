package com.secretapp.backend.api

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.rpc._
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ Ok, Error, RpcRequest }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.{ MessageBox, Package }
import com.secretapp.backend.services.common.PackageCommon._
import scala.util.{ Failure, Success }
import scalaz._
import Scalaz._

object ApiBrokerProtocol {
  sealed trait ApiBrokerMessage

  case class ApiBrokerRequest(connector: ActorRef, messageId: Long, body: RpcRequest) extends ApiBrokerMessage
  case class AuthorizeUser(user: User) extends ApiBrokerMessage
}

class ApiBrokerActor(val currentAuthId: Long, val currentSessionId: Long, val clusterProxies: ClusterProxies, val session: CSession) extends Actor with ActorLogging
    with ApiBrokerService {
  import ApiBrokerProtocol._

  import context._

  val sessionActor = context.parent

  def receive = {
    case AuthorizeUser(user) =>
      log.info(s"AuthorizeUser $user")
      currentUser = Some(user)
    case msg @ ApiBrokerRequest(connector, messageId, body) =>
      log.debug(s"$msg")
      val replyTo = sender()

      handleRpc(messageId)(body) match {
        case \/-(fresp) =>
          fresp onComplete {
            case Success(resp) =>
              replyTo.tell(
                PackageToSend(
                  connector,
                  Package(currentAuthId, currentSessionId, MessageBox(messageId, RpcResponseBox(messageId, resp))).right
                ),
                self
              )
            case Failure(error) =>
              replyTo.tell(
                PackageToSend(
                  connector,
                  Package(currentAuthId, currentSessionId, MessageBox(
                    messageId, RpcResponseBox(messageId, Error(500, "INTERNAL_SERVER_ERROR", error.getMessage, true)))
                  ).right
                ),
                self
              )
              log.error(s"Failed to handle rpc ${connector} ${messageId} ${body}")
          }

        case -\/(UserNotAuthenticated) =>
          replyTo.tell(
            PackageToSend(connector, Package(currentAuthId, currentSessionId, MessageBox(messageId, RpcResponseBox(messageId, Error(401, "USER_NOT_AUTHORIZED", "", true)))).left),
            self
          )
        case -\/(error) =>
          replyTo.tell(
            PackageToSend(connector, Package(currentAuthId, currentSessionId, MessageBox(messageId, RpcResponseBox(messageId, Error(500, "INTERNAL_SERVER_ERROR", error.getMessage, true)))).left),
            self
          )
          log.error(s"Failed to handle rpc ${connector} ${messageId} ${body}")
          throw error
      }
  }
}
