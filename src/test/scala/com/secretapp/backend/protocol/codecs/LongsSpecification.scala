//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits.BitVector
//import org.scalacheck._
//import org.scalacheck.Prop._
//
//object LongsSpecification extends Properties("Longs") {
//
//  val genLong = for {
//    l <- Gen.choose(Long.MinValue, Long.MaxValue)
//    tail <- genLongs
//  } yield Array(l) ++ tail
//  val genLongs: Gen[Array[Long]] = Gen.oneOf(genLong, Gen.const(Array[Long]()))
//
//  property("encodeL") = forAll(genLongs) { (a: Array[Long]) =>
//    val res = Longs.encodeL(a)
//    val bytes = a.foldLeft(BitVector.empty)((acc, l) => acc ++ BitVector.fromLong(l))
//    res == (BitVector(a.length) ++ bytes)
//  }
//
//  property("take") = forAll(genLongs) { (a: Array[Long]) =>
//    val bytes = a.foldLeft(BitVector.empty)((acc, l) => acc ++ BitVector.fromLong(l))
//    val res = Longs.take(BitVector(a.length) ++ bytes)
//    res._1.isEmpty == a.isEmpty || (res._1 == a && res._2 == BitVector.empty)
//  }
//
//}
