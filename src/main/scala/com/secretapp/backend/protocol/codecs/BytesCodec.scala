package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait BytesCodec {

  val protoBytes: Codec[BitVector] = new Codec[BitVector] {

    import ByteConstants._

    def encode(xs: BitVector) = {
      for {
        len <- VarIntCodec.encode(xs.length.toInt / byteSize)
      } yield len ++ xs
    }

    def decode(buf: BitVector) =
      for {
        l <- VarIntCodec.decode(buf) ; (xs, len) = l
      } yield {
        val bitsLen = len * byteSize
        (xs.drop(bitsLen), xs.take(bitsLen))
      }
  }

}

object BytesCodec extends BytesCodec {

  def encode(s: BitVector) = protoBytes.encode(s)

  def decode(buf: BitVector) = protoBytes.decode(buf)

}
