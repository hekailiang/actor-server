package com.secretapp.backend.api

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.UpdatesBroker.NewUpdatePush
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import com.secretapp.backend.persist
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.UserManagerService
import com.secretapp.backend.services.rpc.presence.PresenceService
import com.secretapp.backend.services.rpc.typing.TypingService
import com.secretapp.backend.services.rpc.auth.SignService
import com.secretapp.backend.services.rpc.contact.{ ContactService, PublicKeysService}
import com.secretapp.backend.services.rpc.files.FilesService
import com.secretapp.backend.services.rpc.messaging.MessagingService
import com.secretapp.backend.services.rpc.push.PushService
import com.secretapp.backend.services.rpc.user.UserService
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.api.rpc._
import im.actor.server.persist.file.adapter.FileAdapter
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Success, Failure }
import scalaz._
import Scalaz._

trait ApiError extends Exception
case object UserNotAuthenticated extends ApiError

trait ApiBrokerService extends GeneratorService with UserManagerService with SignService with RpcUpdatesService with MessagingService with ContactService with FilesService
    with PublicKeysService with PresenceService with TypingService with UserService with ActorLogging with PushService {
  self: ApiBrokerActor =>
  import SocialProtocol._

  val sessionActor: ActorRef
  val currentAuthId: Long
  val singletons: Singletons
  val fileAdapter: FileAdapter

  val updatesBrokerRegion: ActorRef
  val socialBrokerRegion: ActorRef

  val subscribedToUpdates: Boolean

  val context: ActorContext
  import context._

  implicit val timeout = Timeout(5.seconds)

  def handleRpc(messageId: Long): PartialFunction[RpcRequest, \/[Throwable, Future[RpcResponse]]] = {
    case Request(body) =>
      runHandler(messageId, body)
  }

  def runHandler(messageId: Long, body: RpcRequestMessage): \/[Throwable, Future[RpcResponse]] = {
    @inline
    def handleUpdates(rq: RpcRequestMessage) = authorizedRequest {
      handleUpdatesRpc(rq)
    }

    body match {
      case rq: updateProto.RequestGetState =>
        handleUpdates(rq)

      case rq: updateProto.RequestGetDifference =>
        handleUpdates(rq)

      case _ =>
        handleRpcMessaging
          .orElse(handleRpcAuth)
          .orElse(handleRpcFiles)
          .orElse(handleRpcContact)
          .orElse(handleRpcPresence)
          .orElse(handleRpcTyping)
          .orElse(handleRpcPublicKeys)
          .orElse(handleRpcUser)
          .orElse(handleRpcPush)(body)
    }
  }

  protected def authorizedRequest(f: => Future[RpcResponse]): \/[Throwable, Future[RpcResponse]] = {
    currentUser map (_ => f.right) getOrElse UserNotAuthenticated.left
  }

  protected def unauthorizedRequest(f: => Future[RpcResponse]): \/[Throwable, Future[RpcResponse]] = {
    f.right
  }

  protected def withAuthIds(userId: Int)(f: Seq[Long] => Unit): Unit =
    persist.AuthId.findAllIdsByUserId(userId) onComplete {
      case Success(authIds) =>
        f(authIds)
      case Failure(e) =>
        log.error(s"Failed to get authIds for uid=$userId to push new device updates")
        throw e
    }

  protected def withRelations(userId: Int)(f: Seq[Long] => Unit): Unit =
    ask(socialBrokerRegion, SocialMessageBox(userId, GetRelations))(5.seconds).mapTo[SocialProtocol.RelationsType] onComplete {
      case Success(userIds) =>
        userIds.foreach(withAuthIds(_)(f))

      case Failure(e) =>
        log.error(s"Failed to get relations for uid=$userId to push new device updates")
        throw e
    }

  protected def pushUpdate(destAuthId: Long, msg: SeqUpdateMessage): Unit =
    updatesBrokerRegion ! NewUpdatePush(destAuthId, msg)
}
