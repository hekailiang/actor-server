package com.secretapp.backend.api

import akka.actor._
import akka.pattern.pipe
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.contrib.pattern.{ ClusterSharding, ShardRegion }
import akka.persistence._
import com.notnoop.apns.ApnsService
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.{persist => p}
import java.util.UUID
import im.actor.util.logging._
import scala.concurrent.Future
import scala.concurrent.duration._
import scodec.bits._

object UpdatesBroker {
  trait UpdateEvent

  case class NewUpdatePush(authId: Long, update: SeqUpdateMessage)

  case class GetSeq(authId: Long)
  case class GetSeqAndState(authId: Long)

  case object Stop

  sealed trait RecoveryEvent
  case object SeqUpdate extends RecoveryEvent

  type State = (Int, Option[UUID]) // seq mid mstate
  type StrictState = (Int, UUID) // seq mid state

  def topicFor(authId: Long): String = s"updates-${authId.toString}"
  def topicFor(authId: String): String = s"updates-$authId"

  private val idExtractor: ShardRegion.IdExtractor = {
    case msg @ NewUpdatePush(authId, _) => (authId.toString, msg)
    case msg @ GetSeq(authId) => (authId.toString, msg)
    case msg @ GetSeqAndState(authId) => (authId.toString, msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    case msg@NewUpdatePush(authId, _) => (authId % shardCount).abs.toString
    case msg@GetSeq(authId) => (authId % shardCount).abs.toString
    case msg@GetSeqAndState(authId) => (authId % shardCount).abs.toString
  }

  def startRegion(apnsService: ApnsService)(implicit system: ActorSystem): ActorRef = ClusterSharding(system).start(
    typeName = "UpdatesBroker",
    entryProps = Some(Props(classOf[UpdatesBroker], apnsService)),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )
}

// TODO: rename to SeqUpdatesBroker
class UpdatesBroker(implicit val apnsService: ApnsService)
    extends PersistentActor with GooglePush with ApplePush with MDCActorLogging {
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

  val receiveCommand: Actor.Receive = {
    case ReceiveTimeout ⇒ context.parent ! Passivate(stopMessage = UpdatesBroker.Stop)
    case UpdatesBroker.Stop => context.stop(self)
    case UpdatesBroker.GetSeq(_) =>
      sender() ! this.seq
    case NewUpdatePush(authId, update) =>
      val replyTo = sender()
      persist(SeqUpdate) { _ =>
        seq += 1
        pushUpdate(authId, seq, update) pipeTo replyTo
        maybeSnapshot()
      }
    case e: SaveSnapshotFailure =>
      log.error("SaveSnapshotFailure {}", e)
  }

  val receiveRecover: Actor.Receive = {
    case RecoveryCompleted =>
    case SnapshotOffer(metadata, offeredSnapshot) =>
      val seq = offeredSnapshot.asInstanceOf[PersistentStateType]
      this.seq = seq
    case SeqUpdate =>
      this.seq += 1
  }

  override protected def mdc = {
    Map("unit" -> "UpdatesBroker")
  }

  private def pushUpdate(authId: Long, updateSeq: Int, update: updateProto.SeqUpdateMessage): Future[StrictState] = {
    // FIXME: Handle errors!
    p.SeqUpdate.push(authId, update) map { uuid =>
      withMDC(Map(
        "authId" -> authId
      )) {
        log.debug(s"Wrote update seq=$updateSeq state=$uuid update=$update")
      }

      if (!update.isInstanceOf[updateProto.MessageSent]) {
        mediator ! Publish(topic, (updateSeq, uuid, update))

        if (update.isInstanceOf[updateProto.Message] || update.isInstanceOf[updateProto.EncryptedMessage])
          deliverGooglePush(authId, updateSeq)

        deliverApplePush(authId, updateSeq, applePushText(update))
      }

      (updateSeq, uuid)
    }
  }

  private def maybeSnapshot(): Unit = {
    if (seq - lastSnapshottedAtSeq >= minSnapshotStep) {
      lastSnapshottedAtSeq = seq
      saveSnapshot(seq)
    }
  }
}
