package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object BytesCodec extends Codec[BitVector] {
  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(xs: BitVector) = {
    for {
      len <- varint.encode(xs.length / byteSize)
    } yield len ++ xs
  }

  def decode(buf: BitVector) = {
    for {
      l <- varint.decode(buf); (xs, len) = l
    } yield {
      val bitsLen = len * byteSize
      (xs.drop(bitsLen), xs.take(bitsLen))
    }
  }
}
