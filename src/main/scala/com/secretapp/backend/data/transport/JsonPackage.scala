package com.secretapp.backend.data.transport

import akka.util.ByteString
import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import scodec.bits._

case class JsonPackage(authId: Long, sessionId: Long, messageBoxBytes: BitVector) extends TransportPackage {
  @deprecated("replyWith should be moved to MessageBox", "")
  def replyWith(messageId: Long, tm: TransportMessage): JsonPackage = {
    println(s"replyWith: ${MessageBox(messageId, tm)}")
    JsonPackage(authId, sessionId, BitVector(MessageBox(messageId, tm).toString.getBytes()))
  }

  def toJson: ByteString = {
    ByteString(s"[${this.authId},${this.sessionId},") ++ ByteString(this.messageBoxBytes.toByteBuffer) ++ ByteString("]")
  }
}
