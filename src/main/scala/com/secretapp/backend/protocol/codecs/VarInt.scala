package com.secretapp.backend.protocol.codecs

import scala.annotation.tailrec
import scala.language.implicitConversions
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import java.nio.ByteBuffer

object VarInt {

  val codec: Codec[Int] = new Codec[Int] {

    def encode(v: Int) = {
      var n: Int = v.abs
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
      def f(buf: ByteVector, position: Int = 0, acc: Int = 0): Int = {
        if (buf.isEmpty) acc
        else {
          val n = ((buf(0) & 0xff) & 0x7f) << 7 * position
          f(buf.drop(1), position + 1, acc ^ n)
        }
      }
      def decodeVI(buf: BitVector) = f(buf.bytes).abs

      val offset = varIntLen(buf)
      val len = decodeVI(buf.take(offset))
      (buf.drop(offset), len).right
    }

    private def sizeOf(buf: Int): Int = buf match {
      case x if x <= 0x7f => 1
      case x if x <= 0x3fff => 2
      case x if x <= 0x1fffff => 3
      case x if x <= 0xfffffff => 4
      case x if x <= 0x7fffffff => 5
      case _ => 6
    }

    private def varIntLen(buf: BitVector): Long = {
      @tailrec
      def f(buf: BitVector, len: Long = 1): Long = {
        if (buf.isEmpty || !buf.head) len
        else f(buf.drop(8), len + 1)
      }
      f(buf) * 8L
    }

  }

  def encode(vi: Int) = codec.encode(vi)

  def decode(buf: BitVector) = codec.decode(buf)

}
