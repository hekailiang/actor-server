package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

object VarIntCodecSpecification extends Properties("VarInt") {
  val integers = Gen.choose(Long.MinValue, Long.MaxValue)

  property("encode/decode") = forAll(integers) { (a: Long) =>
    varint.decode(varint.encode(a).toOption.get) == (BitVector.empty, a.abs).right
  }
}

class VarIntCodecSpecification extends Specification {
  "VarIntCodec" should {
    "encode VarInt" in {
      varint.encode(150) should_== hex"9601".bits.right
      varint.encode(300) should_== hex"ac02".bits.right
      varint.encode(Int.MaxValue) should_== hex"ffffffff07".bits.right
      varint.encode(Int.MinValue + 1) should_== hex"ffffffff07".bits.right
    }

    "decode bytes to VarInt" in {
      varint.decode(hex"9601".bits) should_== (BitVector.empty, 150).right
      varint.decode(hex"9601".bits) should_== (BitVector.empty, 150).right
      varint.decode(hex"ac02".bits) should_== (BitVector.empty, 300).right
      varint.decode(hex"ffffffff07".bits) should_== (BitVector.empty, Int.MaxValue).right
      varint.decode(hex"9601ff".bits) should_== (hex"ff".bits, 150).right
    }
  }
}
