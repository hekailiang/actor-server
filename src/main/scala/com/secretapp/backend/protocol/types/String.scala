package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector

object String {
  def encode(xs: String): ByteVector = {
    val str = xs.getBytes
    val size = VarInt.encode(str.length)
    size ++ ByteVector(str)
  }

  def take(buf: ByteVector): (String, ByteVector) = {
    val (len, xs) = VarInt.take(buf)
    Tuple2(new String(xs.take(len).toArray), xs.drop(len))
  }
}
