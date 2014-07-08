package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait StringCodec {

  val string: Codec[String] = new Codec[String] {

    import ByteConstants._

    def encode(s: String) = {
      val bytes = s.getBytes
      for {
        len <- VarIntCodec.encode(bytes.length)
      } yield len ++ BitVector(bytes)
    }

    def decode(buf: BitVector) =
      for {
        l <- VarIntCodec.decode(buf) ; (xs, len) = l
      } yield {
        val bitsLen = len * longSize
        (xs.drop(bitsLen), new String(xs.take(bitsLen).toByteArray))
      }

  }
}

object StringCodec extends StringCodec {

  def encode(s: String) = string.encode(s)

  def decode(buf: BitVector) = string.decode(buf)

}
