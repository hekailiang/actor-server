package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class UserKey(userId: Int, keyHash: Long) extends ProtobufMessage {
  def toProto = protobuf.UserKey(userId, keyHash)
}

object UserKey {
  def fromProto(userKey: protobuf.UserKey): UserKey = userKey match {
    case protobuf.UserKey(uid, keyHash) => UserKey(uid, keyHash)
  }
}
