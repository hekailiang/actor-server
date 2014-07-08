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

trait VarIntCodec {

  val varint: Codec[Int] = new Codec[Int] {

    import ByteConstants._

    def encode(v: Int) = {
      var n: Int = v.abs
      val res = ByteBuffer.allocate(VarIntCodec.sizeOf(n))
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

      val sizeVI = varIntLen(buf)
      if (sizeVI >= 1 && sizeVI <= 6) {
        val offset = sizeVI * byteSize
        val len = decodeVI(buf.take(offset))
        (buf.drop(offset), len).right
      } else s"Wrong varint size: $sizeVI. Varint should have size within range from 1 to 6.".left
    }

//    TODO: check for max length (6) and return left
    private def varIntLen(buf: BitVector): Int = {
      @tailrec
      def f(buf: BitVector, len: Int = 1): Int = {
        if (buf.isEmpty || !buf.head) len
        else f(buf.drop(byteSize), len + 1)
      }
      f(buf)
    }

  }
}

object VarIntCodec extends VarIntCodec {

  def encode(n: Int) = varint.encode(n)
  def encode(n: Long) = varint.encode(n.toInt)

  def decode(buf: BitVector) = varint.decode(buf)

  def sizeOf(buf: Int): Int = buf match {
    case x if x <= 0x7f => 1
    case x if x <= 0x3fff => 2
    case x if x <= 0x1fffff => 3
    case x if x <= 0xfffffff => 4
    case x if x <= 0x7fffffff => 5
    case _ => 6
  }

}
