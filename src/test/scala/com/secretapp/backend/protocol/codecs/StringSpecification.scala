package com.secretapp.backend.protocol.codecs

import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._

object StringSpecification extends Properties("String") {

  property("encode/decode") = forAll { (a: String) =>
    StringCodec.decode(StringCodec.encode(a).toOption.get) == (BitVector.empty, a).right
  }

}
