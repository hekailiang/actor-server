package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

object SexCodec extends Codec[Sex] {

  private val codec = discriminated[Sex].by(uint8)
    .\ (0) { case n@NoSex => n } (provide(NoSex))
    .\ (1) { case m@Male => m } (provide(Male))
    .\ (2) { case w@Female => w } (provide(Female))

  def encode(s: Sex) = codec.encode(s)

  def decode(buf: BitVector) = codec.decode(buf)

}
