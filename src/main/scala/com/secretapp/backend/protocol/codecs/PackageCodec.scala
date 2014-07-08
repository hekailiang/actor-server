package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import scodec.bits._

trait PackageCodec {
  val packageCodec : Codec[Package] = (int64 :: int64 :: protoMessageWrapper).as[Package]
}

object PackageCodec extends PackageCodec
