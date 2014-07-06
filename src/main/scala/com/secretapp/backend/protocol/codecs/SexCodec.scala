package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

trait SexCodec {
  val sex = discriminated[Sex].by(uint8)
    .\ (0) { case n@NoSex => n } (provide(NoSex))
    .\ (1) { case m@Male => m } (provide(Male))
    .\ (2) { case w@Female => w } (provide(Female))
}

object SexCodec extends SexCodec
