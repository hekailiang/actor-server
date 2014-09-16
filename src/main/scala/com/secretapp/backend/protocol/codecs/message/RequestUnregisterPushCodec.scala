package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message.RequestUnregisterPush
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RequestUnregisterPushCodec extends Codec[RequestUnregisterPush] {

  private val codec = ignore(0)

  override def encode(r: RequestUnregisterPush) = codec.encode(())

  override def decode(buf: BitVector) = codec.decode(buf) map { case (v, _) => (v, RequestUnregisterPush()) }
}
