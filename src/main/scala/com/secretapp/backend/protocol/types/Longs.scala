package com.secretapp.backend.protocol.types

import scodec.bits._
import com.secretapp.backend.protocol.types.{ VarInt => VI }

object Longs {
  def encode(n: Long): ByteVector = BitVector.fromLong(n, 64, ByteOrdering.BigEndian).bytes

  def decode(buf: ByteVector): Long = buf.toLong()

  def encodeL(xs: Array[Long]): ByteVector = {
    val size = VI.encode(xs.length)
    xs.map(encode(_)).foldLeft(size)(_ ++ _)
  }

  def decodeL(buf: ByteVector): Array[Long] = buf.grouped(8).map(decode(_)).toArray

  def take(buf: ByteVector): (Array[Long], ByteVector) = {
    val (len, xs) = VI.take(buf)
    Tuple2(decodeL(xs.take(8 * len)), xs.drop(8 * len))
  }
}
