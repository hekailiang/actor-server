package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._

object StringCodecSpecification extends Properties("String") {

  property("encode/decode") = forAll { (a: String) =>
    protoString.decode(protoString.encode(a).toOption.get) == (BitVector.empty, a).right
  }

}
