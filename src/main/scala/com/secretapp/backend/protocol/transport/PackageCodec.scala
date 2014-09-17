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

object PackageCodec extends Codec[Package] {
  private val codec = (int64 :: int64 :: MessageBoxCodec).as[Package]

  def encode(p: Package) = codec.encode(p)

  def decode(buf: BitVector) = {
    codec.decode(buf)
  }

  def build(authId: Long, sessionId: Long, messageId: Long, message: TransportMessage) = {
    encode(Package(authId, sessionId, MessageBox(messageId, message)))
  }
}
