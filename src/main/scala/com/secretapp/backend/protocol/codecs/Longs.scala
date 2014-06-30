package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object Longs {

  val codec: Codec[Array[Long]] = new Codec[Array[Long]] {

    def encode(a: Array[Long]) = {
      for {
        len <- VarInt.encode(a.length)
      } yield a.map(BitVector.fromLong(_)).foldLeft(len)(_ ++ _)
    }

    def decode(buf: BitVector) =
      for {
        l <- VarInt.decode(buf)
        xs = l._1
        len = l._2 * 8L * 64L
      } yield (xs.drop(len), xs.take(len).grouped(64).map(_.toLong()).toArray)

  }

  def encode(s: Array[Long]) = codec.encode(s)

  def decode(buf: BitVector) = codec.decode(buf)

}
