package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import org.scalacheck._
import org.scalacheck.Prop._
import test.utils.scalacheck.Generators._

object BytesSpecification extends Properties("Bytes") {

  property("encode") = forAll(genBV) { (a: ByteVector) =>
    Bytes.encode(a) == (ByteVector(a.length) ++ a)
    Bytes.take(Bytes.encode(a))._1 == a
  }

  property("take") = forAll(genBV) { (a: ByteVector) =>
    val res = Bytes.take(ByteVector(a.length) ++ a)
    res._1 == a && res._2 == ByteVector.empty

    val resWithTail = Bytes.take(ByteVector(a.length) ++ a ++ a)
    resWithTail._1 == a && resWithTail._2 == a
  }

}
