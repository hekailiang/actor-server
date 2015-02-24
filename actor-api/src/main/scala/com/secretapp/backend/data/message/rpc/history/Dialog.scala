package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.{struct, ProtobufMessage}
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Dialog(
  peer: struct.Peer,
  unreadCount: Int,
  sortDate: Long,
  senderUserId: Int,
  randomId: Long,
  date: Long,
  message: MessageContent,
  state: Option[models.MessageState]
) extends ProtobufMessage {
  def toProto =
    protobuf.Dialog(
      peer.toProto,
      unreadCount,
      sortDate,
      senderUserId,
      randomId,
      date,
      message.toProto,
      state map (s => protobuf.MessageState.valueOf(s.toInt))
    )
}

object Dialog {
  def fromProto(r: protobuf.Dialog) =
    Dialog(
      struct.Peer.fromProto(r.peer),
      r.unreadCount,
      r.sortDate,
      r.senderUid,
      r.rid,
      r.date,
      MessageContent.fromProto(r.message),
      r.state map (s => models.MessageState.fromInt(s.id))
    )
}
