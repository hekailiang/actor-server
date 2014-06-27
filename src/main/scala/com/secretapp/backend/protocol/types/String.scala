package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import com.secretapp.backend.protocol.types.{ VarInt => VI }

object String {
  def encode(xs: String): ByteVector = {
    val str = xs.getBytes
    val size = VI.encode(str.length)
    size ++ ByteVector(str)
  }

  def take(buf: ByteVector): (String, ByteVector) = {
    val (len, xs) = VI.take(buf)
    Tuple2(new String(xs.take(len).toArray), xs.drop(len))
  }
}
