package com.secretapp.backend.protocol.codecs

import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._

sealed trait Sex
case object NoSex extends Sex
case object Man extends Sex
case object Woman extends Sex

object Sex {

  val codec = discriminated[Sex].by(uint8)
    .\ (0) { case n@NoSex => n } (provide(NoSex))
    .\ (1) { case m@Man => m } (provide(Man))
    .\ (2) { case w@Woman => w } (provide(Woman))

}
