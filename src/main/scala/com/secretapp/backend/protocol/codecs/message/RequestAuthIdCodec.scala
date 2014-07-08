package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object RequestAuthIdCodec extends Codec[RequestAuthId] {

  private val codec = provide(RequestAuthId())

  def encode(r: RequestAuthId) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)

}
