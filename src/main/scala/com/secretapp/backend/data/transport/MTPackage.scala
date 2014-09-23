package com.secretapp.backend.data.transport

import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import scodec.bits._

case class MTPackage(authId: Long, sessionId: Long, messageBoxBytes: BitVector) {
  @deprecated("replyWith should be moved to MessageBox", "")
  def replyWith(messageId: Long, tm: TransportMessage): MTPackage = {
    MTPackage(authId, sessionId, MessageBoxCodec.encodeValid(MessageBox(messageId, tm)))
  }
}
