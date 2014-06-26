package com.secretapp.backend.protocol.types

import com.secretapp.backend.protocol.types.{ VarInt => VI }

object Bytes {
  def encode(xs: List[Byte]): List[Byte] = {
    val size = VI.encode(xs.length)
    size ++ xs
  }

  def take(buf: List[Byte]): (List[Byte], List[Byte]) = {
    val (len, xs) = VI.take(buf)
    Tuple2(xs.take(len), xs.drop(len))
  }
}
