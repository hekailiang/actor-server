package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scalaz._
import Scalaz._

object LongsCodecSpecification extends Properties("Longs") {

  val genLong = for {
    l <- Gen.choose(Long.MinValue, Long.MaxValue)
    tail <- genLongs
  } yield Array(l) ++ tail
  val genLongs: Gen[Array[Long]] = Gen.oneOf(genLong, Gen.const(Array[Long]()))

  property("encode/decode") = forAll(genLongs) { (a: Array[Long]) =>
    val res = protoLongs.decode(protoLongs.encode(a).toOption.get)
    (a.isEmpty == res.toOption.get._2.isEmpty) || (res == (BitVector.empty, a).right)
  }

}
