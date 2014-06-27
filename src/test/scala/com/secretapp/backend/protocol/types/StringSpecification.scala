package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import org.scalacheck._
import org.scalacheck.Prop._

object StringSpecification extends Properties("String") {

  property("encode") = forAll { (a: String) =>
    val bytes = ByteVector(a.getBytes)
    String.encode(a) == (VarInt.encode(bytes.length) ++ bytes)
  }

  property("take") = forAll { (a: String) =>
    val bytes = ByteVector(a.getBytes)
    val res = String.take((VarInt.encode(bytes.length) ++ bytes))
    res._1 == a && res._2 == ByteVector.empty
  }

}
