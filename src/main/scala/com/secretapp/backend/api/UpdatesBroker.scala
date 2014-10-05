package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.ClusterSharding
import akka.contrib.pattern.ShardRegion
import akka.event.LoggingReceive
import akka.persistence._
import akka.util.Timeout
import com.gilt.timeuuid.TimeUuid
import com.secretapp.backend.data.message.rpc.messaging.EncryptedMessage
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import java.util.UUID
import scala.concurrent.duration._
import com.datastax.driver.core.{Session => CSession}
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.SeqUpdateRecord
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import scala.concurrent.Future
import scodec.bits._

object UpdatesBroker {
  trait UpdateEvent

  // for events which should be processed by some internal logic inside UpdatesBroker
  case class NewUpdateEvent(authId: Long, event: UpdateEvent)
  case class NewMessageSent(uid: Int, randomId: Long) extends UpdateEvent
  case class NewMessage(senderUID: Int, destUID: Int, keyHash: Long, aesEncryptedKey: BitVector, message: BitVector) extends UpdateEvent

  // for events which should be just pushed to the sequence
  case class NewUpdatePush(authId: Long, update: SeqUpdateMessage)

  case class GetSeq(authId: Long)
  case class GetSeqAndState(authId: Long)

  case object Stop

  type State = (Int, Option[UUID]) // seq mid mstate
  type StrictState = (Int, UUID) // seq mid state

  def topicFor(authId: Long): String = s"updates-${authId.toString}"
  def topicFor(authId: String): String = s"updates-${authId}"

  private val idExtractor: ShardRegion.IdExtractor = {
    case msg @ NewUpdateEvent(authId, _) => (authId.toString, msg)
    case msg @ NewUpdatePush(authId, _) => (authId.toString, msg)
    case msg @ GetSeq(authId) => (authId.toString, msg)
    case msg @ GetSeqAndState(authId) => (authId.toString, msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = msg => msg match {
    case msg @ NewUpdateEvent(authId, _) => (authId % shardCount).toString
    case msg @ NewUpdatePush(authId, _) => (authId % shardCount).toString
    case msg @ GetSeq(authId) => (authId % shardCount).toString
    case msg @ GetSeqAndState(authId) => (authId % shardCount).toString
  }

  def startRegion()(implicit system: ActorSystem, session: CSession): ActorRef = ClusterSharding(system).start(
    typeName = "UpdatesBroker",
    entryProps = Some(Props(new UpdatesBroker)),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )
}

// TODO: rename to SeqUpdatesBroker
class UpdatesBroker(implicit session: CSession) extends PersistentActor with ActorLogging with GooglePush {
  import context.dispatcher
  import ShardRegion.Passivate
  import UpdatesBroker._

  context.setReceiveTimeout(1.day)
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  val mediator = DistributedPubSubExtension(context.system).mediator
  val topic = UpdatesBroker.topicFor(self.path.name)

  var seq: Int = 0

  type PersistentStateType = Int // Seq
  var lastSnapshottedAtSeq: Int = 0
  val minSnapshotStep: Int = 200

  val receiveCommand: Receive = LoggingReceive {
    case ReceiveTimeout ⇒ context.parent ! Passivate(stopMessage = UpdatesBroker.Stop)
    case UpdatesBroker.Stop => context.stop(self)
    case UpdatesBroker.GetSeq(_) =>
      sender() ! this.seq
    case p @ NewUpdatePush(authId, update) =>
      val replyTo = sender()
      log.info(s"NewUpdatePush for $authId: $update")
      persist(p) { _ =>
        seq += 1
        pushUpdate(authId, update) map { reply =>
          replyTo ! reply
        }
        maybeSnapshot()
      }
    case p @ NewUpdateEvent(authId, NewMessageSent(uid, randomId)) =>
      val replyTo = sender()
      log.info(s"NewMessageSent $p from ${replyTo.path}")
      persist(p) { _ =>
        seq += 1
        val update = updateProto.MessageSent(uid, randomId)
        pushUpdate(authId, update) map { reply =>
          replyTo ! reply
        }
        maybeSnapshot()
      }
    case p @ NewUpdateEvent(authId, NewMessage(senderUID, destUID, keyHash, aesEncryptedKey, message)) =>
      val replyTo = sender()
      log.info(s"NewMessage $p from ${replyTo.path}")
      persist(p) { _ =>
        seq += 1
        val update = updateProto.Message(
          senderUID = senderUID,
          destUID = destUID,
          keyHash = keyHash,
          aesEncryptedKey = aesEncryptedKey,
          message = message
        )
        pushUpdate(authId, update)
        deliverGooglePush(destUID, authId, seq)
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
      val seq = offeredSnapshot.asInstanceOf[PersistentStateType]
      this.seq = seq
    case msg @ NewUpdatePush(_, update) =>
      log.debug(s"Recovering NewUpdatePush ${msg}")
      this.seq += 1
    case msg @ NewUpdateEvent(_, NewMessage(_, _, _, _, _)) =>
      log.debug(s"Recovering NewMessage ${msg}")
      this.seq += 1
    case msg @ NewUpdateEvent(_, NewMessageSent(_, _)) =>
      log.debug(s"Recovering NewMessageSent ${msg}")
      this.seq += 1
  }

  private def pushUpdate(authId: Long, update: updateProto.SeqUpdateMessage): Future[StrictState] = {
    // FIXME: Handle errors!
    val updSeq = seq
    val uuid = TimeUuid()

    update match {
      case _: updateProto.MessageSent =>
        log.debug("Not pushing update MessageSent to session")
      case _ =>
        mediator ! Publish(topic, (updSeq, uuid, update))
        log.info(
          s"Published update authId=$authId seq=${this.seq} state=${uuid} update=${update}"
        )
    }

    SeqUpdateRecord.push(uuid, authId, update)(session) map { _ =>
      log.debug(s"Wrote update authId=${authId} seq=${this.seq} state=${uuid} update=${update}")
      (seq, uuid)
    }
  }

  private def maybeSnapshot(): Unit = {
    if (seq - lastSnapshottedAtSeq >= minSnapshotStep) {
      log.debug(s"Saving snapshot seq=${seq} lastSnapshottedAtSeq=${lastSnapshottedAtSeq}")
      lastSnapshottedAtSeq = seq
      saveSnapshot(seq)
    }
  }
}
