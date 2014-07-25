package com.secretapp.backend.services

import akka.actor.Actor
import com.secretapp.backend.api._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.rpc.auth._
import com.secretapp.backend.services.rpc.contact._
import com.secretapp.backend.services.transport._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.api.rpc._

trait RpcService extends SignService with RpcMessagingService with RpcUpdatesService with ContactService {
  self: Actor with PackageManagerService =>

  def handleRpc(p: Package, messageId: Long): PartialFunction[RpcRequest, Unit] = {
    case Request(body) =>
      runHandler(p, messageId, body)
    case RequestWithInit(initConnection, body) =>
      //      TODO: initConnection
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
      //  (handleRequestGetDifference(p, messageId, user) _).tupled(updateProto.RequestGetDifference.unapply(rq).get)

      case _ =>
        handleRpcAuth(p, messageId).
          orElse(handleRpcContact(p, messageId)).
          orElse(handleRpcContact(p, messageId))(body)
    }
  }

  private def authorizedRequest[A](p: Package)(f: (User) => A) = {
    getUser map (f(_)) getOrElse (sendDrop(p, new RuntimeException("user is not authenticated")))
  }
}
