package com.secretapp.backend.services

import akka.actor.Actor
import com.secretapp.backend.api.SocialBroker
import com.secretapp.backend.api.UpdatesBroker
import com.secretapp.backend.api._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.rpc.auth.SignService
import com.secretapp.backend.services.rpc.contact.{ ContactService, PublicKeysService}
import com.secretapp.backend.services.rpc.files.FilesService
import com.secretapp.backend.services.transport._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.api.rpc._

trait RpcService extends SignService with RpcMessagingService with RpcUpdatesService with ContactService with FilesService
with PublicKeysService {
  self: Actor with PackageManagerService =>

  import context.system

  val updatesBrokerRegion = UpdatesBroker.startRegion()
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
        handleRpcAuth(p, messageId).
          orElse(handleRpcFiles(p, messageId)).
          orElse(handleRpcContact(p, messageId)).
          orElse(handleRpcPublicKeys(p, messageId))(body)
    }
  }

  private def authorizedRequest[A](p: Package)(f: (User) => A) = {
    getUser map (f(_)) getOrElse (sendDrop(p, new RuntimeException("user is not authenticated")))
  }
}
