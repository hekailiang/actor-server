package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.presence._
 import com.secretapp.backend.data.transport.MTPackage
 import com.secretapp.backend.protocol.transport._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait PresenceService {
  this: ApiBrokerService =>

  import context.dispatcher
  import context.system

  private lazy val presenceHandler = context.actorOf(Props(new Handler(sessionActor, getUser.get, clusterProxies.presenceBroker)), "presence")

  def handleRpcPresence: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case rq: RequestSetOnline =>
      authorizedRequest {
        (presenceHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
    case rq: SubscribeForOnline =>
      authorizedRequest {
        (presenceHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
    case rq: UnsubscribeForOnline =>
      authorizedRequest {
        (presenceHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
  }
}
