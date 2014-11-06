package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.message._
import scodec.bits._
import scodec.Codec
import scodec.codecs._

object MessageAckCodec extends Codec[MessageAck] {
  private val codec = protoLongs.pxmap[MessageAck](MessageAck.apply, MessageAck.unapply)

  def encode(a: MessageAck) = codec.encode(a)

  def decode(buf: BitVector) = codec.decode(buf)
}
