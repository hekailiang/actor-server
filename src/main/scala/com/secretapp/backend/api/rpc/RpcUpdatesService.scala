package com.secretapp.backend.api.rpc

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.pattern.ask
import com.secretapp.backend.api.UpdatesManager
import com.secretapp.backend.api.UpdatesProtocol
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.services._
import com.secretapp.backend.services.transport._
import java.util.UUID
import scala.concurrent.Future
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class PusherActor(handleActor: ActorRef) extends Actor with ActorLogging {
  lazy val rand = new Random

  def receive = {
    case (u: updateProto.Message, seq: Int, state: UUID) =>
      val upd = CommonUpdate(seq, uuid.encode(state).toOption.get, u)
      //val mb = MessageBox(rand.nextLong, upd)
      //handleActor ! MessageBoxToSend(mb)
  }
}

trait RpcUpdatesService {
  this: RpcService with PackageManagerService with Actor =>

  import context.dispatcher

  lazy val mediator = DistributedPubSubExtension(context.system).mediator
  lazy val pusher = context.actorOf(Props(new PusherActor(handleActor)))

  def handleRequestGetState(p: Package, messageId: Long, currentUser: (Long, User)) = {
    getOrCreateUpdatesManager(currentUser._2.publicKeyHash) map { manager =>
      for {
        s <- ask(manager, UpdatesProtocol.GetState).mapTo[(Int, Option[UUID])]; (seq, muuid) = s
      } yield {
        val rsp = State(seq, muuid map (uuid.encode(_).toOption.get) getOrElse (BitVector.empty))
        mediator ! Subscribe(UpdatesManager.topicFor(currentUser._2.publicKeyHash), pusher)
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
      }
    }
  }
}
