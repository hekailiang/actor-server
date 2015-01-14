package com.secretapp.backend.protocol.codecs.message

import com.eaio.uuid.UUID
import scodec.Codec
import scodec.codecs._
import scodec.bits.BitVector

object EaioUuidCodec extends Codec[UUID] {

  val codec = int64 ~ int64

  override def encode(u: UUID) =
    codec.encode((u.time, u.clockSeqAndNode))

  override def decode(bits: BitVector) =
    codec.decode(bits) map { case (remaining, (m, l)) => (remaining, new UUID(m, l)) }

  override def toString = "eaioUuid"
}
