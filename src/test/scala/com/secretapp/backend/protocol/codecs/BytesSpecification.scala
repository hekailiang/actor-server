package com.secretapp.backend.protocol.codecs

import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._
import test.utils.scalacheck.Generators._

object BytesSpecification extends Properties("Bytes") {

  property("encode/decode") = forAll(genBV) { (a: BitVector) =>
    Bytes.decode(Bytes.encode(a).toOption.get) == (BitVector.empty, a).right
  }

}
