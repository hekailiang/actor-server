package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.{ UpdatesServiceActor, ApiBrokerService }
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.RpcResponse
import scala.concurrent.Future

trait RpcUpdatesService {
  this: ApiBrokerService =>

  lazy val updatesService = context.actorOf(Props(
    new UpdatesServiceActor(context.parent, updatesBrokerRegion, subscribedToUpdates, getUser.get.uid, getUser.get.authId)
  ), "updates-service")

  def handleUpdatesRpc(rq: RpcRequestMessage): Future[RpcResponse] =
    (updatesService ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
}
