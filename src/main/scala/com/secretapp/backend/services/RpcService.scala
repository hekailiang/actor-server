package com.secretapp.backend.services

import akka.pattern.ask
import com.secretapp.backend.api.SocialProtocol.{GetRelations, SocialMessageBox}
import com.secretapp.backend.api.UpdatesBroker.NewUpdatePush
import com.secretapp.backend.api.{SocialBroker, UpdatesBroker, _}
import com.secretapp.backend.api.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{update => updateProto, _}
import com.secretapp.backend.data.message.update.CommonUpdateMessage
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.UserPublicKeyRecord
import com.secretapp.backend.services.rpc.auth.SignService
import com.secretapp.backend.services.rpc.contact.{ContactService, PublicKeysService}
import com.secretapp.backend.services.rpc.files.FilesService
import com.secretapp.backend.services.rpc.presence.PresenceService
import com.secretapp.backend.services.rpc.user.UserService
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait RpcService extends SignService with RpcMessagingService with RpcUpdatesService with ContactService with FilesService
with PublicKeysService with PresenceService with UserService {
  self: ApiHandlerActor =>

  import context.system
  import context._

  val updatesBrokerRegion = UpdatesBroker.startRegion()(context.system, session)
  val socialBrokerRegion = SocialBroker.startRegion()

  def handleRpc(p: Package, messageId: Long): PartialFunction[RpcRequest, Unit] = {
    case Request(body) =>
      runHandler(p, messageId, body)
    case RequestWithInit(initConnection, body) =>
      runHandler(p, messageId, body)
  }

  def runHandler(p: Package, messageId: Long, body: RpcRequestMessage) = {
    @inline
    def handleUpdates(rq: RpcRequestMessage) = authorizedRequest(p) { user =>
      handleUpdatesRpc(user, p, messageId, rq)
    }

    @inline
    def handleMessaging(rq: RpcRequestMessage) = authorizedRequest(p) { user =>
      handleMessagingRpc(user, p, messageId, rq)
    }

    body match {
      case rq: RequestSendMessage =>
        handleMessaging(rq)

      case rq: updateProto.RequestGetState =>
        handleUpdates(rq)

      case rq: updateProto.RequestGetDifference =>
        handleUpdates(rq)

      case _ =>
        handleRpcAuth(p, messageId)
          .orElse(handleRpcFiles(p, messageId))
          .orElse(handleRpcContact(p, messageId))
          .orElse(handleRpcPresence(p, messageId))
          .orElse(handleRpcPublicKeys(p, messageId))(body)
          .orElse(handleRpcUser(p, messageId))(body)
    }
  }

  protected def authorizedRequest[A](p: Package)(f: User => A) = {
    getUser map f getOrElse sendDrop(p, new RuntimeException("user is not authenticated"))
  }

  protected def withAuthIds(uid: Int)(f: Seq[Long] => Unit): Unit =
    UserPublicKeyRecord.fetchAuthIdsByUid(uid) onComplete {
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

  protected def pushUpdate(destAuthId: Long, msg: CommonUpdateMessage): Unit =
    updatesBrokerRegion ! NewUpdatePush(destAuthId, msg)
}
