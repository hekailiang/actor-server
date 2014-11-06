package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs.ByteConstants
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object BooleanCodec extends Codec[Boolean] {
  import ByteConstants._

  def encode(b: Boolean) = b match {
    case true => hex"1".bits.right
    case _ => hex"0".bits.right
  }

  def decode(buf: BitVector) = {
    val res = buf.take(byteSize) != hex"0".bits
    (buf.drop(byteSize), res).right
  }
}
