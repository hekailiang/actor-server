package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.protocol.codecs._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object ErrorCodec extends Codec[Error] {
  private val codec = (int32 :: protoString :: protoString :: protoBool :: protoBytes).as[Error]

  def encode(e: Error) = codec.encode(e)

  def decode(buf: BitVector) = codec.decode(buf)
}
