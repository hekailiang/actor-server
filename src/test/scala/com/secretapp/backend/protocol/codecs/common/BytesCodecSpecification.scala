package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits.BitVector
import org.scalacheck._
import org.scalacheck.Prop._
import test.utils.scalacheck.Generators._
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

object BytesCodecSpecification extends Properties("Bytes") {
  property("encode/decode") = forAll(genBV) { (a: BitVector) =>
    protoBytes.decode(protoBytes.encode(a).toOption.get) == (BitVector.empty, a).right
  }
}

class BytesCodecSpecification extends Specification {
  "BytesCodec" should {
    "encode ByteVector" in {
      val v = hex"f0aff01".bits
      protoBytes.encode(v) should_== (hex"4".bits ++ v).right
    }

    "decode bytes to ByteVector" in {
      val v = hex"f0aff01".bits
      val res = protoBytes.decode(hex"4".bits ++ v).toOption.get
      res._1 should_== BitVector.empty
      res._2 should_== v
    }
  }
}
