package com.secretapp.backend.helpers

import akka.actor._
import com.secretapp.backend.api.DialogManagerProtocol
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.models
import org.joda.time.DateTime
import scala.collection.immutable

trait HistoryHelpers extends UserHelpers {
  import DialogManagerProtocol._
  val dialogManagerRegion: ActorRef

  protected def writeInHistoryMessage(
    userId: Int,
    peer: models.Peer,
    date: DateTime,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent
  ) = writeHistoryMessage(
    userId,
    peer,
    date,
    randomId,
    senderUserId,
    message,
    models.MessageState.Sent
  )

  protected def writeOutHistoryMessage(
    userId: Int,
    peer: models.Peer,
    date: DateTime,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent
  ) = writeHistoryMessage(
    userId,
    peer,
    date,
    randomId,
    senderUserId,
    message,
    models.MessageState.Sent
  )

  protected def writeHistoryMessage(
    userId: Int,
    peer: models.Peer,
    date: DateTime,
    randomId: Long,
    senderUserId: Int,
    message: MessageContent,
    state: models.MessageState,
    updateDialogOrder: Boolean = true
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, WriteMessage(
      date, randomId, senderUserId, message, state, updateDialogOrder
    ))
  }

  protected def noteEncryptedMessage(
    userId: Int,
    peer: models.Peer,
    date: DateTime,
    senderUserId: Int
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, NoteEncryptedMessage(date, senderUserId))
  }

  protected def clearDialogMessages(
    userId: Int,
    peer: models.Peer
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, ClearMessages)
  }

  protected def markOutMessagesReceived(
    userId: Int,
    peer: models.Peer,
    date: DateTime
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, OutMessagesReceived(date))
  }

  protected def markOutMessagesRead(
    userId: Int,
    peer: models.Peer,
    date: DateTime
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, OutMessagesRead(date))
  }

  protected def markInMessagesReceived(
    userId: Int,
    peer: models.Peer,
    date: DateTime
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, InMessagesReceived(date))
  }

  protected def markInMessagesRead(
    userId: Int,
    peer: models.Peer,
    date: DateTime
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, InMessagesRead(date))
  }

  protected def markMessageDeleted(
    userId: Int,
    peer: models.Peer,
    randomIds: immutable.Seq[Long]
  ): Unit = {
    dialogManagerRegion ! Envelope(userId, peer, MessageDelete(randomIds))
  }
}
