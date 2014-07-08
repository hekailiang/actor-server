package com.secretapp.backend.protocol.codecs

import com.secretapp.backend.data._
import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class PackageSpec extends FlatSpec with Matchers {
  "encode" should "pack Package" in {
    val p = Package(1L, 2L, MessageWrapper(3L, RequestAuthId()))
    packageCodec.encode(p) should === {
      hex"230000000000000001000000000000000200000000000000030001f0000000008b5e1a04".bits.right
    }
  }

  "decode" should "unpack bytes to Package" in {
    val v = hex"230000000000000001000000000000000200000000000000030001f0000000008b5e1a04".bits
    val p = Package(1L, 2L, MessageWrapper(3L, RequestAuthId()))
    packageCodec.decode(v) should === ((BitVector.empty, p).right)
  }

}
