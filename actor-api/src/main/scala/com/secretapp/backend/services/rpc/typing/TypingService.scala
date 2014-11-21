package com.secretapp.backend.services.rpc.typing

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.protocol.transport._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait TypingService {
  this: ApiBrokerService =>

  import context.dispatcher
  import context.system

  private lazy val typingHandler = context.actorOf(Props(classOf[Handler], sessionActor, getUser.get, singletons.typingBrokerRegion, session), "typing")

  def handleRpcTyping: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case r: RequestTyping => authorizedRequest {
      (typingHandler ? RpcProtocol.Request(r)).mapTo[RpcResponse]
    }
  }
}
