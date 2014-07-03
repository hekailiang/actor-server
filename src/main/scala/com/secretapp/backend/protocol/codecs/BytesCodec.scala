package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait BytesCodec {

  val bytes: Codec[BitVector] = new Codec[BitVector] {

    def encode(xs: BitVector) = {
      for {
        len <- VarIntCodec.encode(xs.length.toInt / 8)
      } yield len ++ xs
    }

    def decode(buf: BitVector) =
      for {
        l <- VarIntCodec.decode(buf)
        xs = l._1
        len = l._2 * 8L
      } yield (xs.drop(len), xs.take(len))
  }

}

object BytesCodec extends BytesCodec {
  def encode(s: BitVector) = bytes.encode(s)

  def decode(buf: BitVector) = bytes.decode(buf)
}
