package com.secretapp.backend.data.transport

import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.{JsonMessageBoxCodec, MessageBoxCodec}
import com.secretapp.backend.api.frontend._

case class MessageBox(messageId: Long, body: TransportMessage) {
  def replyWith(authId: Long, sessionId: Long, replyMessageId: Long, message: TransportMessage, transport: TransportConnection): TransportPackage = {
    println(s"replying with ${MessageBox(replyMessageId, message)}")
    val mb = MessageBox(replyMessageId, message)
    transport match {
      case BinaryConnection => MTPackage(authId, sessionId, MessageBoxCodec.encodeValid(mb))
      case JsonConnection => JsonPackage(authId, sessionId, JsonMessageBoxCodec.encodeValid(mb))
    }
  }

  @deprecated("messageIs should not depend on client's messageId", "")
  def replyWith(authId: Long, sessionId: Long, message: TransportMessage, transport: TransportConnection): TransportPackage = {
    replyWith(authId, sessionId, messageId, message, transport)
  }
}
