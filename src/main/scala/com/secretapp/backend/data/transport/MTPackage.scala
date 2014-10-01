package com.secretapp.backend.data.transport

import akka.util.ByteString
import com.secretapp.backend.data.message.TransportMessage
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.protocol.transport.MTPackageCodec
import scodec.bits._

case class MTPackage(authId: Long, sessionId: Long, messageBoxBytes: BitVector) extends TransportPackage {
  def decodeMessageBox = MessageBoxCodec.decodeValue(this.messageBoxBytes)

  def encode = ByteString(MTPackageCodec.encodeValid(this).toByteBuffer)
}

object MTPackage {
  def build(authId: Long, sessionId: Long, message: MessageBox) = {
    val mb = MessageBoxCodec.encodeValid(message)
    MTPackage(authId, sessionId, mb)
  }
}
