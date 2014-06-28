package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object Bytes {

  val codec: Codec[BitVector] = new Codec[BitVector] {

    def encode(xs: BitVector) = {
      for {
        len <- VarInt.encode(xs.length.toInt / 8)
      } yield len ++ xs
    }

    def decode(buf: BitVector) =
      for {
        l <- VarInt.decode(buf)
        xs = l._1
        len = l._2 * 8L
      } yield (xs.drop(len), xs.take(len))

  }

  def encode(s: BitVector) = codec.encode(s)

  def decode(buf: BitVector) = codec.decode(buf)

}
