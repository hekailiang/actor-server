package com.secretapp.backend.protocol.transport

import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.message.TransportMessage
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import scodec.bits._

object MTPackageCodec extends Codec[MTPackage] {
  private val codec = (int64 :: int64 :: bits).as[MTPackage]

  def encode(p: MTPackage) = codec.encode(p)

  def decode(buf: BitVector) = {
    codec.decode(buf)
  }

  @deprecated("use encode instead of build", "")
  def build(authId: Long, sessionId: Long, messageId: Long, message: TransportMessage) = {
    encode(MTPackage(authId, sessionId, MessageBoxCodec.encodeValid(MessageBox(messageId, message))))
  }
}
