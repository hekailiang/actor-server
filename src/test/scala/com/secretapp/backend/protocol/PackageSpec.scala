package com.secretapp.backend.protocol

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._
import codecs._
import com.secretapp.backend.data._

class PackageSpec extends FlatSpec with Matchers {

  "encode" should "pack Package" in {
    PackageCodec.encode(1L, 2L, 3L, RequestAuthId()) should === {
      hex"230000000000000001000000000000000200000000000000030001f0000000008b5e1a04".bits.right
    }
  }

  "decode" should "unpack bytes to Package" in {
    val v = hex"230000000000000001000000000000000200000000000000030001f0000000008b5e1a04".bits
    val p = Package(PackageHead(1L, 2L, 3L, 1), RequestAuthId())
    PackageCodec.decode(v) should === ((BitVector.empty, p).right)
  }

}
