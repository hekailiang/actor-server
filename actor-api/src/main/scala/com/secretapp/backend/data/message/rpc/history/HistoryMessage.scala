package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class HistoryMessage(senderUserId: Int, randomId: Long, date: Long, message: MessageContent) extends ProtobufMessage {
  def toProto = protobuf.HistoryMessage(senderUserId, randomId, date, message.toProto)
}

object HistoryMessage {
  def fromProto(r: protobuf.HistoryMessage) =
    HistoryMessage(r.senderUid, r.rid, r.date, MessageContent.fromProto(r.message))
}
