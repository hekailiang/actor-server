package com.secretapp.backend.protocol.codecs.common

import scodec.bits._
import scodec.Codec
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

class PayloadBytesCodec[A](payloadCodec: Codec[A]) extends Codec[A] {
  def encode(v: A) = {
    payloadCodec.encode(v).flatMap(BytesCodec.encode)
  }

  def decode(buf: BitVector) = {
    for {
      b <- BytesCodec.decode(buf); (remain, xs) = b
      p <- payloadCodec.decode(xs); (_, res) = p
    } yield (remain, res)
  }
}
