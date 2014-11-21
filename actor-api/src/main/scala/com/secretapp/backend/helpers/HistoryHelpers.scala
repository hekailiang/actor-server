package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.DialogManagerProtocol
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.persist
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future

trait HistoryHelpers extends UserHelpers {
  import DialogManagerProtocol._
  val dialogManagerRegion: ActorRef

  def writeHistoryMessage(
    userId: Int,
    peer: struct.Peer,
    date: Long,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, WriteMessage(
      date, randomId, senderUserId, message
    ))
  }

  def markMessageRead(
    userId: Int,
    peer: struct.Peer,
    date: Long
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, MessageRead(date))
  }

  def markMessageDeleted(
    userId: Int,
    peer: struct.Peer,
    randomIds: immutable.Seq[Long]
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, MessageDelete(randomIds))
  }
}
