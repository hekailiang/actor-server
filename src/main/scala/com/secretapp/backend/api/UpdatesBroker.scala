package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.event.LoggingReceive
import akka.persistence._
import akka.util.Timeout
import com.gilt.timeuuid.TimeUuid
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
  case class NewMessage(senderUID: Int, destUID: Int, update: EncryptedMessage, aesMessage: Option[BitVector]) extends UpdateEvent
  case class GetSeq(authId: Long)
  case object Stop

  type State = (Int, Int, Option[UUID]) // seq mid mstate
  type StrictState = (Int, Int, UUID) // seq mid state

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

  def startRegion()(implicit system: ActorSystem, session: CSession): ActorRef = ClusterSharding(system).start(
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

  context.setReceiveTimeout(1.day)
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(self.path.name)

  var seq: Int = 0
  var mid: Int = 0

  type PersistentStateType = (Int, Int) // seq mid
  var lastSnapshottedAtSeq: Int = 0
  val minSnapshotStep: Int = 3

  val receiveCommand: Receive = LoggingReceive {
    case ReceiveTimeout ⇒ context.parent ! Passivate(stopMessage = UpdatesBroker.Stop)
    case UpdatesBroker.Stop => context.stop(self)
    case UpdatesBroker.GetSeq(_) =>
      sender() ! this.seq
    case p @ NewUpdateEvent(authId, NewMessageSent(randomId)) =>
      val replyTo = sender()
      log.info(s"NewMessageSent ${p} from ${replyTo.path}")
      persist(p) { _ =>
        seq += 1
        mid += 1
        val update = updateProto.MessageSent(mid, randomId)
        pushUpdate(replyTo, authId, update) map { reply =>
          replyTo ! reply
          log.info(s"Replying to ${replyTo.path} with ${reply}")
        }
        maybeSnapshot()
      }
    case p @ NewUpdateEvent(authId, NewMessage(senderUID, destUID, message, aesMessage)) =>
      val replyTo = sender()
      log.info(s"NewMessage ${p} from ${replyTo.path}")
      persist(p) { _ =>
        seq += 1
        mid += 1
        val update = updateProto.Message(
          senderUID = senderUID,
          destUID = destUID,
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
        maybeSnapshot()
      }
    case s: SaveSnapshotSuccess =>
      log.debug("SaveSnapshotSuccess {}", s)
    case e: SaveSnapshotFailure =>
      log.error("SaveSnapshotFailure {}", e)
  }

  val receiveRecover: Actor.Receive = {
    case RecoveryCompleted =>
    case SnapshotOffer(metadata, offeredSnapshot) =>
      log.debug("SnapshotOffer {} {}", metadata, offeredSnapshot)
      val (seq, mid) = offeredSnapshot.asInstanceOf[PersistentStateType]
      this.seq = seq
      this.mid = mid
    case msg @ NewUpdateEvent(_, NewMessage(_, _, _, _)) =>
      log.debug(s"Recovering NewMessage ${msg}")
      this.seq += 1
      this.mid += 1
    case msg @ NewUpdateEvent(_, NewMessageSent(_)) =>
      log.debug(s"Recovering NewMessageSent ${msg}")
      this.seq += 1
      this.mid += 1
  }

  private def pushUpdate(replyTo: ActorRef, authId: Long, update: updateProto.CommonUpdateMessage): Future[Tuple3[Int, Int, UUID]] = {
    // FIXME: Handle errors!
    val updSeq = seq
    val uuid = TimeUuid()

    update match {
      case _: updateProto.MessageSent =>
        log.debug("Not pushing update MessageSent to session")
      case _ =>
        mediator ! Publish(topic, (updSeq, uuid, update))
        log.info(
          s"Published update authId=${authId} seq=${this.seq} mid=${this.mid} state=${uuid} update=${update}"
        )
    }

    CommonUpdateRecord.push(uuid, authId, update)(session) map { _ =>
      log.debug("Wrote update authId=${authId} seq=${this.seq} mid=${this.mid} state=${uuid} update=${update}")
      (seq, mid, uuid)
    }
  }

  private def maybeSnapshot(): Unit = {
    if (seq - lastSnapshottedAtSeq >= minSnapshotStep) {
      log.debug(s"Saving snapshot seq=${seq} mid=${mid} lastSnapshottedAtSeq=${lastSnapshottedAtSeq}")
      lastSnapshottedAtSeq = seq
      saveSnapshot((seq, mid))
    }
  }
}
