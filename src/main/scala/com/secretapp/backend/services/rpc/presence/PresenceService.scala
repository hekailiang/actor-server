package com.secretapp.backend.services.rpc.presence

import akka.actor._
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services.transport.PackageManagerService

trait PresenceService {
  this: ApiHandlerActor =>

  import context.dispatcher
  import context.system

  private lazy val presenceHandler = context.actorOf(Props(new Handler(handleActor, getUser.get, clusterProxies.presenceBroker)), "presence")

  def handleRpcPresence(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case rq: RequestSetOnline =>
      presenceHandler ! RpcProtocol.Request(p, messageId, rq)
    case rq: SubscribeForOnline =>
      presenceHandler ! RpcProtocol.Request(p, messageId, rq)
    case rq: UnsubscribeForOnline =>
      presenceHandler ! RpcProtocol.Request(p, messageId, rq)
  }
}
