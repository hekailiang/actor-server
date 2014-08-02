package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SharedActors }
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services._
import com.secretapp.backend.services.transport._
import scala.concurrent.Future
import scalaz.Scalaz._
import scodec.codecs.uuid

trait RpcMessagingService {
  this: RpcUpdatesService with PackageManagerService with Actor =>

  import context.dispatcher
  import context.system

  lazy val messagingService = context.actorOf(Props(
    new MessagingServiceActor(handleActor, updatesBrokerRegion, getUser.get, getSessionId)
  ), "messaging-service")

  def handleMessagingRpc(user: User, p: Package, messageId: Long, rq: RpcRequestMessage) = {
    log.info("Handling messaging rpc {} {} {} {}", user, p, messageId, rq)
    messagingService ! RpcProtocol.Request(p, messageId, rq)
  }
}
