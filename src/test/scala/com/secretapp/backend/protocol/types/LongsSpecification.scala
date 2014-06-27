package com.secretapp.backend.protocol.types

import scodec.bits.ByteVector
import org.scalacheck._
import org.scalacheck.Prop._

object LongsSpecification extends Properties("Longs") {

  val genLong = for {
    l <- Gen.choose(Long.MinValue, Long.MaxValue)
    tail <- genLongs
  } yield Array(l) ++ tail
  val genLongs: Gen[Array[Long]] = Gen.oneOf(genLong, Gen.const(Array[Long]()))

  property("encodeL") = forAll(genLongs) { (a: Array[Long]) =>
    val res = Longs.encodeL(a)
    val bytes = a.foldLeft(ByteVector.empty)((acc, l) => acc ++ ByteVector.fromLong(l))
    res == (ByteVector(a.length) ++ bytes)
  }

  property("take") = forAll(genLongs) { (a: Array[Long]) =>
    val bytes = a.foldLeft(ByteVector.empty)((acc, l) => acc ++ ByteVector.fromLong(l))
    val res = Longs.take(ByteVector(a.length) ++ bytes)
    res._1.isEmpty == a.isEmpty || (res._1 == a && res._2 == ByteVector.empty)
  }

}
