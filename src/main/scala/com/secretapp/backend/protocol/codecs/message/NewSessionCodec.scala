package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data._
import scodec.bits._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._

object NewSessionCodec extends Codec[NewSession] {

  private val codec = (int64 :: int64).as[NewSession]

  def encode(ns: NewSession) = codec.encode(ns)

  def decode(buf: BitVector) = codec.decode(buf)

}
