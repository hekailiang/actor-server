package com.secretapp.backend.protocol.codecs.message.rpc.push

import com.secretapp.backend.data.message.rpc.push.RequestRegisterGooglePush
import com.secretapp.backend.protocol.codecs._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestRegisterGooglePushCodec extends Codec[RequestRegisterGooglePush] {

  private val codec = (int64 :: protoString).as[RequestRegisterGooglePush]

  override def encode(r: RequestRegisterGooglePush) = codec.encode(r)

  override def decode(buf: BitVector) = codec.decode(buf)
}
