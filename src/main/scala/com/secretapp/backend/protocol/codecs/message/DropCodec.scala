package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object DropCodec extends Codec[Drop] {

  private val codec = (int64 :: protoString).as[Drop]

  def encode(d : Drop) = codec.encode(d)

  def decode(buf : BitVector) = codec.decode(buf)

}
