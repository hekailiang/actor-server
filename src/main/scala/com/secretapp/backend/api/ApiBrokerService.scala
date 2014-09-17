package com.secretapp.backend.api

import akka.actor._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.UserManagerService
import com.secretapp.backend.services.rpc.presence.PresenceService
import com.secretapp.backend.services.rpc.auth.SignService
import com.secretapp.backend.services.rpc.contact.{ ContactService, PublicKeysService}
import com.secretapp.backend.services.rpc.files.FilesService
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.api.rpc._
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz._
import Scalaz._

trait ApiError extends Exception
case object UserNotAuthenticated extends ApiError

trait ApiBrokerService extends GeneratorService with UserManagerService with SignService with RpcUpdatesService with RpcMessagingService with ContactService with FilesService
with PublicKeysService with PresenceService with ActorLogging {
  self: ApiBrokerActor =>

  val clusterProxies: ClusterProxies
  val sessionActor: ActorRef
  val currentAuthId: Long

  val context: ActorContext
  import context.system

  implicit val timeout = Timeout(5.seconds)

  // FIXME: move to service init. Now it creates regions on each session creation
  val updatesBrokerRegion = UpdatesBroker.startRegion()(context.system, session)
  val socialBrokerRegion = SocialBroker.startRegion()

  def handleRpc(messageId: Long): PartialFunction[RpcRequest, \/[Throwable, Future[RpcResponse]]] = {
    case Request(body) =>
      runHandler(messageId, body)
    case RequestWithInit(initConnection, body) =>
      runHandler(messageId, body)
  }

  def runHandler(messageId: Long, body: RpcRequestMessage): \/[Throwable, Future[RpcResponse]] = {
    @inline
    def handleUpdates(rq: RpcRequestMessage) = authorizedRequest {
      handleUpdatesRpc(rq)
    }

    @inline
    def handleMessaging(rq: RpcRequestMessage) = authorizedRequest {
      handleMessagingRpc(rq)
    }

    body match {
      case rq: RequestSendMessage =>
        handleMessaging(rq)

      case rq: updateProto.RequestGetState =>
        handleUpdates(rq)

      case rq: updateProto.RequestGetDifference =>
        handleUpdates(rq)

      case _ =>
        handleRpcAuth.
          orElse(handleRpcFiles).
          orElse(handleRpcContact).
          orElse(handleRpcPresence).
          orElse(handleRpcPublicKeys)(body)
    }
  }

  protected def authorizedRequest(f: => Future[RpcResponse]): \/[Throwable, Future[RpcResponse]] = {
    currentUser map (_ => f.right) getOrElse (UserNotAuthenticated).left
  }

  protected def unauthorizedRequest(f: => Future[RpcResponse]): \/[Throwable, Future[RpcResponse]] = {
    f.right
  }
}
