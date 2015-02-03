package com.secretapp.backend.api

import akka.actor._
import akka.pattern.pipe
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.contrib.pattern.{ ClusterSharding, ShardRegion }
import akka.persistence._
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.rpc.messaging.TextMessage
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.models
import com.secretapp.backend.{ persist => p }
import java.util.UUID
import im.actor.util.logging._
import org.joda.time.DateTime
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Success, Failure }
import scodec.bits._

object DialogManagerProtocol {
  sealed trait DialogMessage

  @SerialVersionUID(1L)
  case class Envelope(userId: Int, peer: models.Peer, payload: DialogMessage)

  @SerialVersionUID(1L)
  case class WriteMessage(
    date: DateTime,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent,
    state: models.MessageState,
    updateDialogOrder: Boolean
  ) extends DialogMessage

  case class NoteEncryptedMessage(
    date: DateTime,
    senderUserId: Int
  ) extends DialogMessage

  @SerialVersionUID(1L)
  case class OutMessagesReceived(date: DateTime) extends DialogMessage

  @SerialVersionUID(1L)
  case class OutMessagesRead(date: DateTime) extends DialogMessage

  @SerialVersionUID(1L)
  case class InMessagesReceived(date: DateTime) extends DialogMessage

  @SerialVersionUID(1L)
  case class InMessagesRead(date: DateTime) extends DialogMessage

  @SerialVersionUID(1L)
  case class MessageDelete(randomIds: immutable.Seq[Long]) extends DialogMessage
}

object DialogManager {
  import DialogManagerProtocol._

  case object Stop

  private val idExtractor: ShardRegion.IdExtractor = {
    case msg @ Envelope(userId, peer, payload) => ("$userId-${peer.typ.intType}-${peer.id}", msg)
  }

  private val shardCount = 2 // TODO: configurable

  private val shardResolver: ShardRegion.ShardResolver = {
    case Envelope(userId, _, _) => (userId % shardCount).abs.toString
  }

  def startRegion()(implicit system: ActorSystem): ActorRef = ClusterSharding(system).start(
    typeName = "DialogManager",
    entryProps = Some(Props(classOf[DialogManager])),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )
}

class DialogManager extends Actor with Stash with ActorLogging {
  import context.dispatcher
  import ShardRegion.Passivate
  import DialogManagerProtocol._

  private case object Unstash

  context.setReceiveTimeout(15.minutes)

  var lastDate = new DateTime

  def receive = business

  def stashing: Receive = {
    case Unstash =>
      unstashAll()
      context.become(business, discardOld = true)
    case msg =>
      stash()
  }

  protected def newSortDate(date: DateTime): DateTime = {
    val newDate = if (date == lastDate) {
      date.plusMillis(1)
    } else {
      date
    }

    lastDate = newDate
    newDate
  }

  def business: Receive = {
    case Envelope(userId, peer, WriteMessage(date, randomId, senderUserId, message, state, updateDialogOrder)) =>
      val sortDate = newSortDate(date)

      val sortDateOpt = if (updateDialogOrder) {
        Some(sortDate)
      } else {
        None
      }

      val f = Future.sequence(Seq(
        p.HistoryMessage.create(
          userId,
          peer,
          date,
          randomId,
          senderUserId,
          message.header,
          message.content,
          state
        ),
        p.Dialog.createOrUpdate(
          userId,
          peer,
          sortDateOpt,
          senderUserId,
          Some(randomId),
          Some(date),
          Some((message.header, message.content)),
          state
        )
      ))

      backToBusinessAfter(f)

      context.become(stashing)
    case Envelope(userId, peer, NoteEncryptedMessage(date, senderUserId)) =>
      val sortDate = newSortDate(date)

      p.Dialog.createOrUpdate(
        userId,
        peer,
        Some(sortDate),
        senderUserId,
        None,
        None,
        None,
        models.MessageState.Sent
      )
    case Envelope(userId, peer, OutMessagesReceived(date)) =>
      val f = Future.sequence(Seq(
        p.HistoryMessage.updateStateOfSentBefore(userId, peer, date, models.MessageState.Received),
        p.Dialog.updateStateIfFresh(userId, peer, userId, date, models.MessageState.Received)
      ))

      backToBusinessAfter(f)

      context.become(stashing)
    case Envelope(userId, peer, OutMessagesRead(date)) =>
      val f = Future.sequence(Seq(
        p.HistoryMessage.updateStateOfSentBefore(userId, peer, date, models.MessageState.Read),
        p.Dialog.updateStateIfFresh(userId, peer, userId, date, models.MessageState.Read)
      ))

      backToBusinessAfter(f)

      context.become(stashing)
    case Envelope(userId, peer, InMessagesReceived(date)) =>
      val f = p.HistoryMessage.updateStateOfReceivedBefore(userId, peer, date, models.MessageState.Received)

      backToBusinessAfter(f)

      context.become(stashing)
    case Envelope(userId, peer, InMessagesRead(date)) =>
      val f = p.HistoryMessage.updateStateOfReceivedBefore(userId, peer, date, models.MessageState.Read)

      backToBusinessAfter(f)

      context.become(stashing)
    case Envelope(userId, peer, MessageDelete(randomIds)) =>
      randomIds map (p.HistoryMessage.destroy(userId, peer, _))
  }

  def backToBusinessAfter(f: Future[Any]): Unit = f onComplete {
    case Success(_) => backToBusiness()
    case Failure(e) =>
      log.error(e, "Future failed")
  }

  def backToBusiness(): Unit = {
    self ! Unstash
  }
}
