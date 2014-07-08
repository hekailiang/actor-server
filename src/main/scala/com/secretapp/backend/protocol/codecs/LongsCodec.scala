package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait LongsCodec {
  val longs: Codec[Array[Long]] = new Codec[Array[Long]] {

    import ByteConstants._

    def encode(a: Array[Long]) = {
      for {
        len <- VarIntCodec.encode(a.length)
      } yield a.map(BitVector.fromLong(_)).foldLeft(len)(_ ++ _)
    }

    def decode(buf: BitVector) =
      for {
        l <- VarIntCodec.decode(buf) ; (xs, len) = l
      } yield {
        val bitsLen = len * longSize
        (xs.drop(bitsLen), xs.take(bitsLen).grouped(longSize).map(_.toLong()).toArray)
      }

  }
}

object LongsCodec extends LongsCodec {

  def encode(s: Array[Long]) = longs.encode(s)

  def decode(buf: BitVector) = longs.decode(buf)

}
