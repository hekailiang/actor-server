package com.secretapp.backend.protocol.codecs

import scodec.bits._
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

class DiscriminatedErrorCodec[T, B](codecName: String) extends Codec[T] {
  import ByteConstants._

  def encode(a: T) = s"$codecName.type is unknown for ${a.getClass.getCanonicalName}. You should add codec for that.".left

  def decode(buf: BitVector) = s"$codecName.type is unknown. Body: '${buf.toHex}', length: ${buf.length / byteSize}".left
}
