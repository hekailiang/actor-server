package com.secretapp.backend.protocol.codecs.transport

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport._
import org.specs2.mutable.Specification
import scodec.bits._
import scalaz._
import Scalaz._

class PackageBoxCodecSpec extends Specification {
  "PackageBoxCodec" should {
    "encode Package" in {
      protoPackageBox.build(1L, 2L, 3L, RequestAuthId()) should_== {
        hex"1e00000000000000010000000000000002000000000000000301f013bb3636".bits.right
      }
    }

    "decode bytes to Package" in {
      val v = hex"1e00000000000000010000000000000002000000000000000301f013bb3636".bits
      val p = PackageBox(Package(1L, 2L, MessageBox(3L, RequestAuthId())))
      protoPackageBox.decode(v) should_== (BitVector.empty, p).right
    }
  }
}
