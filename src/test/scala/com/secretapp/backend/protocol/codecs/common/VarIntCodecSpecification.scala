package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._

object VarIntCodecSpecification extends Properties("VarInt") {

  val integers = Gen.choose(Int.MinValue, Int.MaxValue)

  property("encode/decode") = forAll(integers) { (a: Int) =>
    varint.decode(varint.encode(a).toOption.get) == (BitVector.empty, a.abs).right
  }

}
