package com.secretapp.backend.protocol.types

import scodec.bits._

object Longs {
  def encode(n: Long): ByteVector = ByteVector.fromLong(n)

  def decode(buf: ByteVector): Long = buf.toLong()

  def encodeL(xs: Array[Long]): ByteVector = {
    val size = VarInt.encode(xs.length)
    xs.map(encode(_)).foldLeft(size)(_ ++ _)
  }

  private def decodeL(buf: ByteVector): Array[Long] = buf.grouped(8).map(decode(_)).toArray

  def take(buf: ByteVector): (Array[Long], ByteVector) = {
    val (len, xs) = VarInt.take(buf)
    Tuple2(decodeL(xs.take(8 * len)), xs.drop(8 * len))
  }
}
