package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.models
import scodec.bits._
import scodec.Codec
import scodec.codecs._
import scalaz._
import Scalaz._

object SexCodec extends Codec[models.Sex] {

  private val codec = discriminated[models.Sex].by(uint8)
    .\ (0) { case n@models.NoSex => n } (provide(models.NoSex))
    .\ (1) { case m@models.Male => m } (provide(models.Male))
    .\ (2) { case w@models.Female => w } (provide(models.Female))

  def encode(s: models.Sex) = codec.encode(s)

  def decode(buf: BitVector) = codec.decode(buf)

}
