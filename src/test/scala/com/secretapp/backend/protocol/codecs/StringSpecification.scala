//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits.BitVector
//import org.scalacheck._
//import org.scalacheck.Prop._
//
//object StringSpecification extends Properties("String") {
//
//  property("encode") = forAll { (a: String) =>
//    val bytes = BitVector(a.getBytes)
//    String.encode(a) == (VarInt.encode(bytes.length / 8) ++ bytes)
//  }
//
//  property("take") = forAll { (a: String) =>
//    val bytes = BitVector(a.getBytes)
//    val res = String.take((VarInt.encode(bytes.length / 8) ++ bytes))
//    res._1 == a && res._2 == BitVector.empty
//  }
//
//}
