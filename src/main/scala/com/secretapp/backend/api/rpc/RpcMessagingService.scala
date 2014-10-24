package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MTPackage
import com.secretapp.backend.services._
import com.secretapp.backend.protocol.transport._
import scala.concurrent.Future
import scalaz.Scalaz._
import scodec.codecs.uuid

trait RpcMessagingService {
  this: ApiBrokerService =>

  import context.dispatcher
  import context.system

  lazy val messagingService = context.actorOf(Props(
    new MessagingServiceActor(updatesBrokerRegion, socialBrokerRegion, fileRecord, clusterProxies.filesCounterProxy, currentUser.get)
  ), "messaging-service")

  def handleMessagingRpc(rq: RpcRequestMessage): Future[RpcResponse] = {
    (messagingService ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
  }
}
