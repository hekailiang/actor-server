package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector

object Bytes {
  def encode(xs: ByteVector): ByteVector = VarInt.encode(xs.length) ++ xs

  def take(buf: ByteVector): (ByteVector, ByteVector) = {
    val (len, xs) = VarInt.take(buf)
    Tuple2(xs.take(len), xs.drop(len))
  }
}
