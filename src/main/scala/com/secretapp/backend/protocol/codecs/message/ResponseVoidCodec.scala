package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message.ResponseVoid
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object ResponseVoidCodec extends Codec[ResponseVoid] {

  private val codec = ignore(0)

  override def encode(r: ResponseVoid) = codec.encode(())

  override def decode(buf: BitVector) = codec.decode(buf) map { case (v, _) => (v, ResponseVoid()) }
}
