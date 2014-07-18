package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

object StringCodecSpecification extends Properties("String") {
  property("encode/decode") = forAll { (a: String) =>
    protoString.decode(protoString.encode(a).toOption.get) == (BitVector.empty, a).right
  }
}

class StringCodecSpecification extends Specification {
  "StringCodec" should {
    "encode string" in {
      protoString.encode("strтестΩ≈ç√") should_== hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a".bits.right
      protoString.encode("") should_== hex"0".bits.right
    }

    "decode bytes to string" in {
      val res = protoString.decode(hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a".bits).toOption.get
      res._1 should_== BitVector.empty
      res._2 should_== "strтестΩ≈ç√"

      val resWithTail = protoString.decode(hex"0000000000000000647fffffffffffffff".bits).toOption.get
      resWithTail._1 should_== hex"00000000000000647fffffffffffffff".bits
      resWithTail._2 should_== ""
    }
  }
}
