package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

case class ChatId(chatId: Int, accessHash: Long) extends ProtobufMessage {
  def toProto = protobuf.ChatId(chatId, accessHash)
}

object ChatId {
  def fromProto(chatId: protobuf.ChatId): ChatId = chatId match {
    case protobuf.ChatId(chatId, accessHash) => ChatId(chatId, accessHash)
  }
}
