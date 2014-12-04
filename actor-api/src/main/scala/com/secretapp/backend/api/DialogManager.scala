package com.secretapp.backend.api

import akka.actor._
import akka.pattern.pipe
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{ Publish, Subscribe }
import akka.contrib.pattern.{ ClusterSharding, ShardRegion }
import akka.persistence._
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.update.SeqUpdateMessage
import com.secretapp.backend.data.message.{update => updateProto}
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.{persist => p}
import java.util.UUID
import im.actor.util.logging._
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scodec.bits._

object DialogManagerProtocol {
  sealed trait DialogMessage

  @SerialVersionUID(1L)
  case class Envelope(userId: Int, peer: struct.Peer, payload: DialogMessage)

  @SerialVersionUID(1L)
  case class WriteMessage(
    date: Long,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent,
    state: struct.MessageState
  ) extends DialogMessage

  @SerialVersionUID(1L)
  case class MessageRead(date: Long) extends DialogMessage

  @SerialVersionUID(1L)
  case class MessageReceived(date: Long) extends DialogMessage

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

  def startRegion()(implicit system: ActorSystem, session: CSession): ActorRef = ClusterSharding(system).start(
    typeName = "DialogManager",
    entryProps = Some(Props(classOf[DialogManager], session)),
    idExtractor = idExtractor,
    shardResolver = shardResolver
  )
}

class DialogManager(implicit val session: CSession) extends Actor {
  import context.dispatcher
  import ShardRegion.Passivate
  import DialogManagerProtocol._

  context.setReceiveTimeout(15.minutes)

  var lastDate = System.currentTimeMillis

  def receive = {
    case Envelope(userId, peer, WriteMessage(date, randomId, senderUserId, message, state)) =>
      val newDate = if (date == lastDate) {
        date + 1
      } else {
        date
      }
      lastDate = newDate
      p.HistoryMessage.insertEntity(userId, peer, newDate, randomId, senderUserId, message, state)
      p.DialogUnreadCounter.increment(userId, peer)
      p.Dialog.updateEntity(userId, peer, senderUserId, randomId, newDate, message)
    case Envelope(userId, peer, MessageRead(date)) =>
      p.HistoryMessage.fetchCountBefore(userId, peer, date) map (p.DialogUnreadCounter.decrement(userId, peer, _))
      setMessagesState(userId, peer, date, struct.MessageState.Read)
    case Envelope(userId, peer, MessageReceived(date)) =>
      setMessagesState(userId, peer, date, struct.MessageState.Received)
    case Envelope(userId, peer, MessageDelete(randomIds)) =>
      randomIds map (p.HistoryMessage.setDeleted(userId, peer, _))
  }

  def setMessagesState(userId: Int, peer: struct.Peer, date: Long, state: struct.MessageState): Unit = {
    peer.typ match {
      case struct.PeerType.Private =>
        p.HistoryMessage.setState(userId, peer, date, state)
        p.HistoryMessage.setState(peer.id, struct.Peer.privat(userId), date, state)
      case struct.PeerType.Group =>
        // TODO: use cache
        p.GroupUser.getUserIds(peer.id) map { groupUserIds =>
          for (groupUserId <- groupUserIds)
            p.HistoryMessage.setState(groupUserId, struct.Peer.group(peer.id), date, state)
        }
    }
  }
}
