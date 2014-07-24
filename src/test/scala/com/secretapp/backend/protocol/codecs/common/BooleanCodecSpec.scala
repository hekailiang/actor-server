package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

object BooleanCodecSpec extends Properties("Boolean") {
  property("encode/decode") = forAll { (b: Boolean) =>
    protoBool.decode(protoBool.encode(b).toOption.get) == (BitVector.empty, b).right
  }
}

class BooleanCodecSpec extends Specification {
  "BooleanCodecSpec" should {
    "encode Boolean" in {
      val v = true
      protoBool.encode(v) should_== (hex"1".bits).right
    }

    "decode bytes to Boolean" in {
      println(s"toOption.get: ${protoBool.decode(hex"0".bits).toOption.get}")
      protoBool.decode(hex"0".bits).toOption.get should_== (BitVector.empty, false)
      protoBool.decode(hex"1".bits).toOption.get should_== (BitVector.empty, true)
      protoBool.decode(hex"ff".bits).toOption.get should_== (BitVector.empty, true)
    }
  }
}
