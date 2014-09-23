package com.secretapp.backend.data.transport

import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec

case class MessageBox(messageId: Long, body: TransportMessage) {
  def replyWith(authId: Long, sessionId: Long, replyMessageId: Long, message: TransportMessage): MTPackage = {
    println(s"replying with ${MessageBox(replyMessageId, message)}")
    MTPackage(authId, sessionId, MessageBoxCodec.encodeValid(MessageBox(replyMessageId, message)))
  }

  @deprecated("messageIs should not depend on client's messageId", "")
  def replyWith(authId: Long, sessionId: Long, message: TransportMessage): MTPackage = {
    replyWith(authId, sessionId, messageId, message)
  }
}
