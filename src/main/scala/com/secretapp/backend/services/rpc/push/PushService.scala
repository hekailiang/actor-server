package com.secretapp.backend.services.rpc.push

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.{RpcResponse, RpcRequestMessage}
import com.secretapp.backend.data.message.rpc.push.{RequestUnregisterPush, RequestRegisterGooglePush}
import com.secretapp.backend.data.transport.Package

trait PushService {
  this: ApiBrokerService =>

  lazy val handler = context.actorOf(Props(new Handler(currentUser.get)), "pushes")

  def handleRpcPush(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r @ (_: RequestRegisterGooglePush | _: RequestUnregisterPush) =>
      authorizedRequest {
        (handler ? RpcProtocol.Request(r)).mapTo[RpcResponse]
      }
  }

}
