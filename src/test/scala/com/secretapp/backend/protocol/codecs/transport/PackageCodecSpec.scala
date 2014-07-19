package com.secretapp.backend.protocol.codecs.transport

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport._
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

class PackageCodecSpec extends Specification {
  "PackageCodec" should {
    "encode Package" in {
      protoPackage.build(1L, 2L, 3L, RequestAuthId()) should_== {
        hex"00000000000000010000000000000002000000000000000301f0".bits.right
      }
    }

    "decode bytes to Package" in {
      val v = hex"00000000000000010000000000000002000000000000000301f0".bits
      val p = Package(1L, 2L, MessageBox(3L, RequestAuthId()))
      protoPackage.decode(v) should_== (BitVector.empty, p).right
    }
  }
}
