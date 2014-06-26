package com.secretapp.backend.protocol.types

import com.secretapp.backend.protocol.types.{ VarInt => VI }

object String {
  def encode(xs: String): List[Byte] = {
    val str = xs.getBytes
    val size = VI.encode(str.length)
    size ++ str
  }

  def take(buf: List[Byte]): (String, List[Byte]) = {
    val (len, xs) = VI.take(buf)
    Tuple2(new String(xs.take(len).toArray), xs.drop(len))
  }
}
