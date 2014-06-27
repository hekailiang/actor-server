package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import com.secretapp.backend.protocol.types.{ VarInt => VI }

object Bytes {
  def encode(xs: ByteVector): ByteVector = VI.encode(xs.length) ++ xs

  def take(buf: ByteVector): (ByteVector, ByteVector) = {
    val (len, xs) = VI.take(buf)
    Tuple2(xs.take(len), xs.drop(len))
  }
}
