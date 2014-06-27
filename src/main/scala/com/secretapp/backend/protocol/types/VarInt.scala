package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import java.nio.ByteBuffer

object VarInt {

  private def sizeOf(buf: Int): Int = buf match {
    case x if x <= 0x7f => 1
    case x if x <= 0x3fff => 2
    case x if x <= 0x1fffff => 3
    case x if x <= 0xfffffff => 4
    case x if x <= 0x7fffffff => 5
    case x if x <= Int.MaxValue => 6
  }

  def encode(buf: Int): ByteVector = {
    val res = ByteBuffer.allocate(sizeOf(buf))
    var n: Int = buf
    while (n > 0x7f) {
      res.put(((n & 0xff) | 0x80).toByte)
      n >>= 7
    }
    res.put(n.toByte)
    res.flip
    ByteVector(res)
  }

  def decode(buf: ByteVector): Int = {
    var position: Int = 0
    var res: Int = 0
    for (n <- buf) {
      res ^= ((n & 0xff) & 0x7f) << 7 * position
      position += 1
    }
    res
  }

  def varIntLen(xs: ByteVector): Int = {
    val len = xs.length
    var i = 0
    while (i < len && (xs(i) & 0xff) > 0x7f) i+= 1
    i + 1
  }

  def take(xs: ByteVector): (Int, ByteVector) = {
    val len = varIntLen(xs)
    Tuple2(decode(xs.take(len)), xs.drop(len))
  }
  
}
