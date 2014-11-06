package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

case class GroupId(groupId: Int, accessHash: Long) extends ProtobufMessage {
  def toProto = protobuf.GroupId(groupId, accessHash)
}

object GroupId {
  def fromProto(groupId: protobuf.GroupId): GroupId = groupId match {
    case protobuf.GroupId(groupId, accessHash) => GroupId(groupId, accessHash)
  }
}
