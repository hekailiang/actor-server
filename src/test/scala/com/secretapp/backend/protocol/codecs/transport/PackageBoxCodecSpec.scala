package com.secretapp.backend.protocol.codecs.transport

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import org.specs2.mutable.Specification
import scodec.bits._
import scalaz._
import Scalaz._

class PackageBoxCodecSpec extends Specification {
  "PackageBoxCodec" should {
    "encode Package" in {
      protoPackageBox.build(0, 1L, 2L, 3L, RequestAuthId()) should_== {
        hex"000000220000000000000000000000010000000000000002000000000000000301f0725ce574".bits.right
      }
    }

    "decode bytes to Package" in {
      val v = hex"000000220000000000000000000000010000000000000002000000000000000301f0725ce574".bits
      val p = MTPackageBox(0, MTPackage(1L, 2L, MessageBoxCodec.encodeValid(MessageBox(3L, RequestAuthId()))))
      protoPackageBox.decode(v) should_== (BitVector.empty, p).right
    }
  }
}
