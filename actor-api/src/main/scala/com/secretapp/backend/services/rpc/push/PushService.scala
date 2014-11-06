package com.secretapp.backend.services.rpc.push

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.{ RpcResponse, RpcRequestMessage }
import com.secretapp.backend.data.message.rpc.push._
import scalaz._

import scala.concurrent.Future

trait PushService {
  this: ApiBrokerService =>

  lazy val handler = context.actorOf(Props(new Handler(currentAuthId)), "pushes")

  val handleRpcPush: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r @ (_: RequestRegisterGooglePush |
              _: RequestRegisterApplePush  |
              _: RequestUnregisterPush     ) => unauthorizedRequest {
      (handler ? RpcProtocol.Request(r)).mapTo[RpcResponse]
    }
  }

}
