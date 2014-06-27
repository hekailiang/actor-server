package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import org.scalacheck._
import org.scalacheck.Prop._

object VarIntSpecification extends Properties("VarInt") {

  val posInteger = Gen.choose(0, Int.MaxValue)

  property("encode") = forAll(posInteger) { (a: Int) =>
    VarInt.decode(VarInt.encode(a)) == a
  }


  val genLSB = for {
    n <- Gen.choose(1, 0x7f)
  } yield ByteVector(n.toByte)
  val genMSB = for {
    n <- Gen.choose(0x80, 0xff)
    tail <- genBV
  } yield ByteVector(n.toByte) ++ tail
  val genBV: Gen[ByteVector] = Gen.resize(8, Gen.oneOf(genMSB, genLSB))

  property("decode") = forAll(genBV) { (a: ByteVector) =>
    VarInt.decode(a) == VarInt.decode(VarInt.encode(VarInt.decode(a)))
  }


  property("take") = forAll(genBV) { (a: ByteVector) =>
    val res = VarInt.take(a)
    res._1 == VarInt.decode(a) && res._2 == ByteVector.empty
  }


  property("varIntLen") = forAll(genBV) { (a: ByteVector) =>
    VarInt.varIntLen(a) == a.length
  }


}
