package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object PingCodec extends Codec[Ping] {

  private val codec = int64.pxmap[Ping](Ping.apply, Ping.unapply)

  def encode(p : Ping) = codec.encode(p)

  def decode(buf : BitVector) = codec.decode(buf)

}
