package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._
import _root_.test.utils.scalacheck.Generators._

object BytesCodecSpecification extends Properties("Bytes") {

  property("encode/decode") = forAll(genBV) { (a: BitVector) =>
    protoBytes.decode(protoBytes.encode(a).toOption.get) == (BitVector.empty, a).right
  }

}
