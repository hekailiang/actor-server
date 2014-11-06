package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object UnsentMessageCodec extends Codec[UnsentMessage] {
  private val codec = (int64 :: int32).as[UnsentMessage]

  def encode(m: UnsentMessage) = codec.encode(m)

  def decode(buf: BitVector) = codec.decode(buf)
}
