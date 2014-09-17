package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object ResponseAuthIdCodec extends Codec[ResponseAuthId] {

  private val codec = int64.pxmap[ResponseAuthId](ResponseAuthId.apply, ResponseAuthId.unapply)

  def encode(r: ResponseAuthId) = codec.encode(r)

  def decode(buf: BitVector) = codec.decode(buf)

}
