package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object PongCodec extends Codec[Pong] {

  private val codec = int64.pxmap[Pong](Pong.apply, Pong.unapply)

  def encode(p : Pong) = codec.encode(p)

  def decode(buf : BitVector) = codec.decode(buf)

}
