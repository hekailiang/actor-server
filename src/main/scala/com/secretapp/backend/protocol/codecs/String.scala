package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object String {

  val codec: Codec[String] = new Codec[String] {

    def encode(s: String) = {
      val bytes = s.getBytes
      for {
        len <- VarInt.encode(bytes.length)
      } yield len ++ BitVector(bytes)
    }

    def decode(buf: BitVector) =
      for {
        l <- VarInt.decode(buf)
        xs = l._1
        len = l._2 * 8
      } yield (xs.drop(len.toLong), new String(xs.take(len).toByteArray))

  }

  def encode(s: String) = codec.encode(s)

  def decode(buf: BitVector) = codec.decode(buf)

}
