package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.persistence._
import akka.util.Timeout
import com.secretapp.backend.data.message.rpc.messaging.EncryptedMessage
import java.util.UUID
import scala.concurrent.duration._
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.CommonUpdateRecord
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import scala.concurrent.Future
import scodec.bits._

object UpdatesBroker {
  trait UpdateEvent
  case class NewUpdateEvent(authId: Long, event: UpdateEvent)
  case class NewMessageSent(randomId: Long) extends UpdateEvent
  case class NewMessage(senderUID: Int, update: EncryptedMessage, aesMessage: Option[BitVector]) extends UpdateEvent
  case class GetSeq(authId: Long)
  case object Stop

  type State = (Int, Int, UUID)

  def topicFor(authId: Long): String = s"updates-${authId.toString}"
  def topicFor(authId: String): String = s"updates-${authId}"

  private val idExtractor: ShardRegion.IdExtractor = {
    case msg @ NewUpdateEvent(authId, _) => (authId.toString, msg)
    case msg @ GetSeq(authId) => (authId.toString, msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case msg @ NewUpdateEvent(authId, _) => (authId % shardCount).toString
    case msg @ GetSeq(authId) => (authId % shardCount).toString
  }

  def region(implicit system: ActorSystem, session: CSession): ActorRef = ClusterSharding(system).start(
    typeName = "UpdatesBroker",
    entryProps = Some(Props(new UpdatesBroker)),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )
}

class UpdatesBroker(implicit session: CSession) extends PersistentActor with ActorLogging {
  import context.dispatcher
  import ShardRegion.Passivate
  import UpdatesBroker._

  context.setReceiveTimeout(120.seconds)
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(self.path.name)

  var seq: Int = 0
  var mid: Int = 0

  val receiveCommand: Receive = {
    case ReceiveTimeout ⇒ context.parent ! Passivate(stopMessage = UpdatesBroker.Stop)
    case UpdatesBroker.Stop => context.stop(self)
    case UpdatesBroker.GetSeq(_) =>
      sender ! this.seq
    case p @ NewUpdateEvent(authId, NewMessageSent(randomId)) =>
      log.info(s"NewMessageSent ${p}")
      val replyTo = sender
      persist(p) { _ =>
        seq += 1
        mid += 1
        val update = updateProto.MessageSent(mid, randomId)
        pushUpdate(replyTo, authId, update)
      }
    case p @ NewUpdateEvent(authId, NewMessage(senderUID, message, aesMessage)) =>
      log.info(s"NewMessage ${p}")
      val replyTo = sender
      persist(p) { _ =>
        seq += 1
        mid += 1
        val update = updateProto.Message(
          senderUID = senderUID,
          destUID = message.uid,
          mid = mid,
          keyHash = message.publicKeyHash,
          useAesKey = aesMessage.isDefined,
          aesKey = message.aesEncryptedKey,
          message = aesMessage match {
            case Some(message) => message
            case None => message.message.get
          }
        )
        pushUpdate(replyTo, authId, update)
      }
  }

  def receiveRecover: Actor.Receive = {
    case RecoveryCompleted =>
    case msg @ NewUpdateEvent(_, NewMessage(_, _, _)) =>
      println(s"Recovering NewMessage ${msg}")
      this.seq += 1
      this.mid += 1
    case msg @ NewUpdateEvent(_, NewMessageSent(_)) =>
      println(s"Recovering NewMessageSent ${msg}")
      this.seq += 1
      this.mid += 1
  }

  private def pushUpdate(replyTo: ActorRef, authId: Long, update: updateProto.CommonUpdateMessage) = {
    // FIXME: Handle errors!
    CommonUpdateRecord.push(authId, update)(session) map { uuid =>
      mediator ! Publish(topic, update)
      replyTo ! (seq, mid, uuid)
      log.info(
        s"Pushed update authId=$authId seq=${this.seq} mid=${this.mid} state=${uuid} update=${update}"
      )
    }
  }
}
