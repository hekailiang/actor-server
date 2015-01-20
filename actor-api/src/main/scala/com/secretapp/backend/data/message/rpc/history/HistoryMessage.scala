package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class HistoryMessage(
  senderUserId: Int,
  randomId: Long,
  date: Long,
  message: MessageContent,
  state: Option[models.MessageState]
) extends ProtobufMessage {
  def toProto = protobuf.HistoryMessage(
    senderUserId,
    randomId,
    date,
    message.toProto,
    state map (s => protobuf.MessageState.valueOf(s.toInt))
  )
}

object HistoryMessage {
  def fromModel(hm: models.HistoryMessage): HistoryMessage = HistoryMessage(
    senderUserId = hm.senderUserId,
    randomId = hm.randomId,
    date = hm.date.getMillis,
    message = MessageContent.build(hm.messageContentHeader, hm.messageContentData),
    state = if (hm.userId == hm.senderUserId) {
      Some(hm.state) // for outgoing
    } else {
      None // for incoming
    }
  )

  def fromProto(r: protobuf.HistoryMessage) =
    HistoryMessage(
      r.senderUid,
      r.rid,
      r.date,
      MessageContent.fromProto(r.message),
      r.state map (s => models.MessageState.fromInt(s.id))
    )
}
