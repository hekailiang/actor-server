package com.secretapp.backend.protocol.codecs.common

import scala.annotation.tailrec
import scala.language.implicitConversions
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import java.nio.ByteBuffer

object VarIntCodec extends Codec[Long] {

  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(v: Long) = {
    var n = v.abs
    val res = ByteBuffer.allocate(sizeOf(n))
    while (n > 0x7f) {
      res.put(((n & 0xff) | 0x80).toByte)
      n >>= 7
    }
    res.put(n.toByte)
    res.flip
    BitVector(res).right
  }

  def decode(buf: BitVector) = {
    @tailrec
    def f(buf: ByteVector, position: Int = 0, acc: Long = 0): Long = {
      if (buf.isEmpty) {
        acc
      } else {
        val n = ((buf(0) & 0xffL) & 0x7fL) << 7 * position
        f(buf.drop(1), position + 1, acc ^ n)
      }
    }
    def decodeVI(buf: BitVector) = f(buf.bytes).abs

    val sizeVI = varIntLen(buf)
    if (sizeVI >= 1 && sizeVI <= maxSize) {
      val offset = sizeVI * byteSize
      val len = decodeVI(buf.take(offset))
      (buf.drop(offset), len).right
    } else {
      s"Wrong varint size: $sizeVI. Varint should have size within range from 1 to $maxSize.".left
    }
  }

  private val maxSize = 10

  def sizeOf(buf: Long): Int = buf match {
    case x if x <= 0x7fL => 1
    case x if x <= 0x3fffL => 2
    case x if x <= 0x1fffffL => 3
    case x if x <= 0xfffffffL => 4
    case x if x <= 0x7fffffffL => 5
    case x if x <= 0x7ffffffffL => 6
    case x if x <= 0x3ffffffffffL => 7
    case x if x <= 0x1ffffffffffffL => 8
    case x if x <= 0xffffffffffffffL => 9
    case _ => maxSize
  }

  private def varIntLen(buf: BitVector): Int = {
    @tailrec
    def f(buf: BitVector, len: Int = 1): Int = {
      if (buf.isEmpty || !buf.head) {
        len
      } else {
        f(buf.drop(byteSize), len + 1)
      }
    }
    f(buf)
  }

}
