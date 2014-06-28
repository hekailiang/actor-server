//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits.BitVector
//import org.scalacheck._
//import org.scalacheck.Prop._
//import test.utils.scalacheck.Generators._
//
//object BytesSpecification extends Properties("Bytes") {
//
//  property("encode") = forAll(genBV) { (a: BitVector) =>
//    Bytes.encode(a) == (BitVector(a.length) ++ a)
//    Bytes.take(Bytes.encode(a))._1 == a
//  }
//
//  property("take") = forAll(genBV) { (a: BitVector) =>
//    val res = Bytes.take(BitVector(a.length / 8) ++ a)
//    res._1 == a && res._2 == BitVector.empty
//
//    val resWithTail = Bytes.take(BitVector(a.length / 8) ++ a ++ a)
//    resWithTail._1 == a && resWithTail._2 == a
//  }
//
//}
