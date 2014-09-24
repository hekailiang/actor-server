package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

object LongsCodecSpec extends Properties("Longs") {
  val genLong = for {
    l <- Gen.choose(Long.MinValue, Long.MaxValue)
    tail <- genLongs
  } yield Vector(l) ++ tail
  val genLongs: Gen[Vector[Long]] = Gen.oneOf(genLong, Gen.const(Vector[Long]()))

  property("encode/decode") = forAll(genLongs) { (a: Vector[Long]) =>
    val res = protoLongs.decode(protoLongs.encode(a).toOption.get)
    (a.isEmpty == res.toOption.get._2.isEmpty) || (res == (BitVector.empty, a).right)
  }
}

class LongsCodecSpec extends Specification {
  "LongsCodec" should {
    "encode array of longs" in {
      protoLongs.encode(Vector(100L, Long.MaxValue)) should_== hex"0200000000000000647fffffffffffffff".bits.right
      protoLongs.encode(Vector[Long]()) should_== hex"0".bits.right
    }

    "decode bytes to array of longs" in {
      val res = protoLongs.decode(hex"0200000000000000647fffffffffffffff".bits).toOption.get
      res._1 should_== BitVector.empty
      res._2 should_== Vector(100L, Long.MaxValue)

      val resWithTail = protoLongs.decode(hex"0000000000000000647fffffffffffffff".bits).toOption.get
      resWithTail._1 should_== hex"00000000000000647fffffffffffffff".bits
      resWithTail._2 should_== Vector[Long]()
    }
  }
}
