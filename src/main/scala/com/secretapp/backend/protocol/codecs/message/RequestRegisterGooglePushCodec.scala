package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message.RequestRegisterGooglePush
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RequestRegisterGooglePushCodec extends Codec[RequestRegisterGooglePush] {

  private val codec = (int32 :: utf8).as[RequestRegisterGooglePush]

  def encode(r: RequestRegisterGooglePush) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)
}
