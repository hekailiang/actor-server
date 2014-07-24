package com.secretapp.backend.protocol.codecs.message.rpc

import com.secretapp.backend.data.message.rpc.FloodWait
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object FloodWaitCodec extends Codec[FloodWait] {
  private val codec = int32.pxmap[FloodWait](FloodWait.apply, FloodWait.unapply)

  def encode(f: FloodWait) = codec.encode(f)

  def decode(buf: BitVector) = codec.decode(buf)
}
