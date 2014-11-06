package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import im.actor.messenger.{ api => protobuf }
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class FastThumb(w: Int, h: Int, thumb: BitVector) extends ProtobufMessage
{
  def toProto = protobuf.FastThumb(w, h, thumb)
}

object FastThumb {
  def fromProto(t: protobuf.FastThumb): FastThumb = t match {
    case protobuf.FastThumb(w, h, thumb) => FastThumb(w, h, thumb)
  }
}
