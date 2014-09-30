package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

case class UserId(uid: Int, accessHash: Long) extends ProtobufMessage {
  def toProto = protobuf.UserId(uid, accessHash)
}

object UserId {
  def fromProto(userId: protobuf.UserId): UserId = userId match {
    case protobuf.UserId(uid, accessHash) => UserId(uid, accessHash)
  }
}
