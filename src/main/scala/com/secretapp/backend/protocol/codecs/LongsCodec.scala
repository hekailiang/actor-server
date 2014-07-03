package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait LongsCodec {
  val longs: Codec[Array[Long]] = new Codec[Array[Long]] {

    def encode(a: Array[Long]) = {
      for {
        len <- VarIntCodec.encode(a.length)
      } yield a.map(BitVector.fromLong(_)).foldLeft(len)(_ ++ _)
    }

    def decode(buf: BitVector) =
      for {
        l <- VarIntCodec.decode(buf)
        xs = l._1
        len = l._2 * 8L * 64L
      } yield (xs.drop(len), xs.take(len).grouped(64).map(_.toLong()).toArray)

  }
}

object LongsCodec extends LongsCodec {
  def encode(s: Array[Long]) = longs.encode(s)

  def decode(buf: BitVector) = longs.decode(buf)

}
