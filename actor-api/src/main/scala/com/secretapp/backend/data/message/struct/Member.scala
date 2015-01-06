package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class Member(
  id: Int,
  inviterUserId: Int,
  date: Long
) extends ProtobufMessage {
  lazy val toProto = protobuf.Member(
    id,
    inviterUserId,
    date
  )
}

object Member {
  def fromProto(m: protobuf.Member): Member =
    Member(m.uid, m.inviterUid, m.date)
}
