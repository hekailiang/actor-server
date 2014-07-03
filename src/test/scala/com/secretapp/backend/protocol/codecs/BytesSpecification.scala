package com.secretapp.backend.protocol.codecs

import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._
import test.utils.scalacheck.Generators._
import com.secretapp.backend.protocol._

object BytesSpecification extends Properties("Bytes") {

  property("encode/decode") = forAll(genBV) { (a: BitVector) =>
    bytes.decode(bytes.encode(a).toOption.get) == (BitVector.empty, a).right
  }

}
