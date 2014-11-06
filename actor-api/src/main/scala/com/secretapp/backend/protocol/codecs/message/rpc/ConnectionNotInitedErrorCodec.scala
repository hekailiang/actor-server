package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc.ConnectionNotInitedError
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object ConnectionNotInitedErrorCodec extends Codec[ConnectionNotInitedError] {
  private val codec = provide(ConnectionNotInitedError())

  def encode(c: ConnectionNotInitedError) = codec.encode(c)

  def decode(buf: BitVector) = codec.decode(buf)
}
