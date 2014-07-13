package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object RequestResendCodec extends Codec[RequestResend] {
  private val codec = int64.pxmap[RequestResend](RequestResend.apply, RequestResend.unapply)

  def encode(r : RequestResend) = codec.encode(r)

  def decode(buf : BitVector) = codec.decode(buf)
}
