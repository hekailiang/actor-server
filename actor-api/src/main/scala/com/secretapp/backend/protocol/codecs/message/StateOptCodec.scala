package com.secretapp.backend.protocol.codecs.message

import scodec.bits.BitVector
import scodec.Codec
import scodec.codecs._
import java.util.UUID
import scalaz._
import Scalaz._

object StateOptCodec extends Codec[Option[UUID]] {
  def encode(u: Option[UUID]) = u match {
    case Some(value) => uuid.encodeValid(value).right
    case None => BitVector.empty.right
  }

  def decode(buf: BitVector) = {
    if (buf.isEmpty) (buf, None).right
    else uuid.decode(buf).map { case (b, value) => (b, value.some) }
  }
}
