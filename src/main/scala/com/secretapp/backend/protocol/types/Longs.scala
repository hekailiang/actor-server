package com.secretapp.backend.protocol.types

import com.secretapp.backend.protocol.types.{ VarInt => VI }
import com.google.common.primitives.{ Longs => L }

object Longs {
  def encode(n: Long): List[Byte] = L.toByteArray(n).toList

  def decode(buf: List[Byte]) = L.fromByteArray(buf.toArray)

  def encodeL(xs: List[Long]): List[Byte] = {
    val size = VI.encode(xs.length)
    val longs = xs.flatMap(encode(_))
    size ++ longs
  }

  def decodeL(buf: List[Byte]): List[Long] = {
    if (buf.isEmpty) {
      Nil
    } else {
      decode(buf.take(8)) :: decodeL(buf.drop(8))
    }
  }

  def take(buf: List[Byte]): (List[Long], List[Byte]) = {
    val (len, xs) = VI.take(buf)
    Tuple2(decodeL(xs.take(8 * len)), xs.drop(8 * len))
  }
}
