package com.secretapp.backend.api

import akka.actor.Actor
import com.secretapp.backend.api.auth._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.api.rpc._

trait RpcService extends SignService with RpcMessagingService { self: Actor with PackageManagerService =>
  def handleRpc(p: Package, messageId: Long): PartialFunction[RpcRequest, Unit] = {
    case Request(body) =>
      runHandler(p, messageId, body)
    case RequestWithInit(initConnection, body) =>
//      TODO: initConnection
      runHandler(p, messageId, body)
  }

  def runHandler(p: Package, messageId: Long, body: RpcRequestMessage) = body match {
    case rq: RequestSendMessage =>
      getUser map { user =>
        (handleRequestSendMessage(p, messageId, user) _).tupled(RequestSendMessage.unapply(rq).get)
      } getOrElse {
        sendDrop(p, "user is not authenticated")
      }

    case _ =>
      handleRpcAuth(p, messageId)(body)
  }
}
