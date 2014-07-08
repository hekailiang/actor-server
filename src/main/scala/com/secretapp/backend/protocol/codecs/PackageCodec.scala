package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import com.secretapp.backend.protocol._
import scodec.{ Codec, DecodingContext }
import scodec.codecs._
import shapeless._
import scalaz._
import Scalaz._
import java.util.zip.CRC32
import scodec.bits._

trait PackageCodec {
  val packageCodec : Codec[Package] = (int64 :: int64 :: protoMessageWrapper).as[Package]
}
