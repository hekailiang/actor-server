package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message.rpc.InternalError
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object InternalErrorCodec extends Codec[InternalError] {
  private val codec = (protoBool :: int32).as[InternalError]

  def encode(e: InternalError) = codec.encode(e)

  def decode(buf: BitVector) = codec.decode(buf)
}
