package com.secretapp.backend.protocol.codecs.message.rpc.push

import com.secretapp.backend.data.message.rpc.push.RequestRegisterApplePush
import com.secretapp.backend.protocol.codecs._
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestRegisterApplePushCodec extends Codec[RequestRegisterApplePush] {

  private val codec = (int32 :: protoString).as[RequestRegisterApplePush]

  override def encode(r: RequestRegisterApplePush) = codec.encode(r)

  override def decode(buf: BitVector) = codec.decode(buf)
}
