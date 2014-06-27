package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import org.scalacheck._
import org.scalacheck.Prop._
import test.utils.scalacheck.Generators._

object VarIntSpecification extends Properties("VarInt") {

  val integers = Gen.choose(Int.MinValue + 1, Int.MaxValue)

  property("encode") = forAll(integers) { (a: Int) =>
    VarInt.decode(VarInt.encode(a)) == a.abs
  }

  property("decode") = forAll(genBV) { (a: ByteVector) =>
    VarInt.decode(a) == VarInt.decode(VarInt.encode(VarInt.decode(a)))
  }

  property("take") = forAll(genBV) { (a: ByteVector) =>
    val res = VarInt.take(a)
    res._1 == VarInt.decode(a) && res._2 == ByteVector.empty

    val resWithTail = VarInt.take(a ++ a)
    resWithTail._1 == VarInt.decode(a) && resWithTail._2 == a
  }

  property("varIntLen") = forAll(genBV) { (a: ByteVector) =>
    VarInt.varIntLen(a) == a.length
  }

}
