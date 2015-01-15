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
import org.joda.time.DateTime
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
    message: MessageContent
  ) extends DialogMessage

  @SerialVersionUID(1L)
  case class MessageRead(date: Long) extends DialogMessage

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
    case Envelope(userId, peer, WriteMessage(date, randomId, senderUserId, message: MessageContent)) =>
      val newDate = if (date == lastDate) {
        date + 1
      } else {
        date
      }
      lastDate = newDate
      p.HistoryMessage.create(userId, peer, new DateTime(newDate), randomId, senderUserId, message)
      p.Dialog.createOrUpdate(userId, peer, senderUserId, randomId, new DateTime(newDate), message)
    case Envelope(userId, peer, MessageRead(date)) =>
      p.HistoryMessage.markAllAsRead(userId, peer, new DateTime(date))
    case Envelope(userId, peer, MessageDelete(randomIds)) =>
      randomIds map (p.HistoryMessage.destroy(userId, peer, _))
  }
}
