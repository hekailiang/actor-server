package com.secretapp.backend.protocol.types

object VarInt {
  def encode(buf: Int): List[Byte] = {
    if (buf > 0x7f) {
      val n = ((buf & 0xff) | 0x80).toByte
      n :: encode(buf >> 7)
    } else {
      buf.toByte :: Nil
    }
  }

  def decode(buf: List[Byte], position: Int = 0): Int = buf match {
    case x :: xs =>
      val n = ((x & 0xff) & 0x7f) << 7 * position
      n ^ decode(xs, position + 1)
    case Nil => 0
  }

  def varIntLen(xs: List[Byte]): Int = {
    xs.takeWhile(x => (x & 0xff) > 0x7f).length + 1
  }

  def take(xs: List[Byte]): (Int, List[Byte]) = {
    val len = varIntLen(xs)
    Tuple2(decode(xs.take(len)), xs.drop(len))
  }
  
}
