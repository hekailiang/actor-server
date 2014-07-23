package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ CounterProtocol, CounterActor, UpdatesBroker, UpdatesManager, SharedActors }
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist._
import com.secretapp.backend.services._
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.services.transport._
import java.util.UUID
import scala.concurrent.Future
import scala.util._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

trait RpcMessagingService {
  this: RpcUpdatesService with PackageManagerService with Actor =>

  import context.dispatcher
  import context.system

  lazy val messagingManager = context.actorOf(Props(new MessagingManager(handleActor, getUser.get)), "messaging")

  def handleMessagingRpc(user: User, p: Package, messageId: Long, rq: RpcRequestMessage) = {
    log.info("Handling messaging rpc {} {} {} {}", user, p, messageId, rq)
    messagingManager ! RpcProtocol.Request(p, messageId, rq)
  }

  // TODO: cache result
  protected def updatesManager(uid: Int, keyHash: Long): Future[ActorRef] = {
    val path = s"updates-manager-${keyHash.toString}"
    SharedActors.lookup(path) {
      system.actorOf(Props(new UpdatesManager(handleActor, uid, keyHash)), path)
    }
  }
}
