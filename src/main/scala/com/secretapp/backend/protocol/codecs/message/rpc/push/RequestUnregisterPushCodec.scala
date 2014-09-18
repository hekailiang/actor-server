package com.secretapp.backend.protocol.codecs.message.rpc.push

import com.secretapp.backend.data.message.rpc.push.RequestUnregisterPush
import scodec.Codec
import scodec.bits._
import scodec.codecs._

object RequestUnregisterPushCodec extends Codec[RequestUnregisterPush] {

  private val codec = ignore(0)

  override def encode(r: RequestUnregisterPush) = codec.encode()

  override def decode(buf: BitVector) = codec.decode(buf) map { case (v, _) => (v, RequestUnregisterPush()) }
}
