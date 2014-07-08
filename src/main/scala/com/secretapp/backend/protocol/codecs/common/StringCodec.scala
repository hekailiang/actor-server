package com.secretapp.backend.protocol.codecs.common

import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import com.secretapp.backend.protocol.codecs._

object StringCodec extends Codec[String] {

  import com.secretapp.backend.protocol.codecs.ByteConstants._

  def encode(s: String) = {
    val bytes = s.getBytes
    for {
      len <- varint.encode(bytes.length)
    } yield len ++ BitVector(bytes)
  }

  def decode(buf: BitVector) = {
    for {
      l <- varint.decode(buf); (xs, len) = l
    } yield {
      val bitsLen = len * longSize
      (xs.drop(bitsLen), new String(xs.take(bitsLen).toByteArray))
    }
  }

}
