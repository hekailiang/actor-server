package com.secretapp.backend.api

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.UpdatesBroker.NewUpdatePush
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.persist.UserPublicKeyRecord
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.UserManagerService
import com.secretapp.backend.services.rpc.presence.PresenceService
import com.secretapp.backend.services.rpc.auth.SignService
import com.secretapp.backend.services.rpc.push.PushService
import com.secretapp.backend.services.rpc.user.UserService
import com.secretapp.backend.services.rpc.contact.{ ContactService, PublicKeysService}
import com.secretapp.backend.services.rpc.files.FilesService
import com.secretapp.backend.protocol.transport._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.api.rpc._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Success, Failure }
import scalaz._
import Scalaz._

trait ApiError extends Exception
case object UserNotAuthenticated extends ApiError

trait ApiBrokerService extends GeneratorService with UserManagerService with SignService with RpcUpdatesService with RpcMessagingService with ContactService with FilesService
with PublicKeysService with PresenceService with UserService with ActorLogging with PushService {
  self: ApiBrokerActor =>
  import SocialProtocol._

  val clusterProxies: ClusterProxies
  val sessionActor: ActorRef
  val currentAuthId: Long
  val singletons: Singletons

  val subscribedToUpdates: Boolean

  val context: ActorContext
  import context._

  implicit val timeout = Timeout(5.seconds)

  // FIXME: move to service init. Now it creates regions on each session creation
  val updatesBrokerRegion = UpdatesBroker.startRegion()(context.system, session)
  val socialBrokerRegion = SocialBroker.startRegion()

  def handleRpc(messageId: Long): PartialFunction[RpcRequest, \/[Throwable, Future[RpcResponse]]] = {
    case Request(body) =>
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

      case rq: RequestCreateChat =>
        handleMessaging(rq)

      case rq: RequestSendGroupMessage =>
        handleMessaging(rq)

      case rq: RequestMessageReceived =>
        handleMessaging(rq)

      case rq: RequestMessageRead =>
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
          orElse(handleRpcPublicKeys).
          orElse(handleRpcUser).
          orElse(handleRpcPush)(body)
    }
  }

  protected def authorizedRequest(f: => Future[RpcResponse]): \/[Throwable, Future[RpcResponse]] = {
    currentUser map (_ => f.right) getOrElse UserNotAuthenticated.left
  }

  protected def unauthorizedRequest(f: => Future[RpcResponse]): \/[Throwable, Future[RpcResponse]] = {
    f.right
  }

  protected def withAuthIds(uid: Int)(f: Seq[Long] => Unit): Unit =
    UserPublicKeyRecord.fetchAuthIdsByUid(uid)(session) onComplete {
      case Success(authIds) =>
        log.debug(s"Fetched authIds for uid=$uid $authIds")
        f(authIds)

      case Failure(e) =>
        log.error(s"Failed to get authIds for uid=$uid to push new device updates")
        throw e
    }

  protected def withRelations(uid: Int)(f: Seq[Long] => Unit): Unit =
    ask(socialBrokerRegion, SocialMessageBox(uid, GetRelations))(5.seconds).mapTo[SocialProtocol.RelationsType] onComplete {
      case Success(uids) =>
        log.debug(s"Got relations for $uid -> $uids")
        uids.foreach(withAuthIds(_)(f))

      case Failure(e) =>
        log.error(s"Failed to get relations for uid=$uid to push new device updates")
        throw e
    }

  protected def pushUpdate(destAuthId: Long, msg: SeqUpdateMessage): Unit =
    updatesBrokerRegion ! NewUpdatePush(destAuthId, msg)
}
