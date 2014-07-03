package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait StringCodec {

  val string: Codec[String] = new Codec[String] {

    def encode(s: String) = {
      val bytes = s.getBytes
      for {
        len <- VarIntCodec.encode(bytes.length)
      } yield len ++ BitVector(bytes)
    }

    def decode(buf: BitVector) =
      for {
        l <- VarIntCodec.decode(buf)
        xs = l._1
        len = l._2 * 8L
      } yield (xs.drop(len), new String(xs.take(len).toByteArray))

  }
}

object StringCodec extends StringCodec {
  def encode(s: String) = string.encode(s)

  def decode(buf: BitVector) = string.decode(buf)
}
