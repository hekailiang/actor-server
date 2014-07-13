package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object UnsentResponseCodec extends Codec[UnsentResponse] {
  private val codec = (int64 :: int64 :: int32).as[UnsentResponse]

  def encode(m : UnsentResponse) = codec.encode(m)

  def decode(buf : BitVector) = codec.decode(buf)
}
