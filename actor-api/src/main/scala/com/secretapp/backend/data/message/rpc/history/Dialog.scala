package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.{struct, ProtobufMessage}
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Dialog(peer: struct.Peer,
                  unreadCount: Int,
                  sortDate: Long,
                  senderUid: Int,
                  randomId: Long,
                  date: Long,
                  message: MessageContent) extends ProtobufMessage {
  def toProto =
    protobuf.ResponseDialogs.Dialog(peer.toProto, unreadCount, sortDate, senderUid, randomId, date, message.toProto)
}

object Dialog {
  def fromProto(r: protobuf.ResponseDialogs.Dialog) =
    Dialog(struct.Peer.fromProto(r.peer), r.unreadCount, r.sortDate, r.senderUid, r.rid, r.date,
      MessageContent.fromProto(r.message))
}
