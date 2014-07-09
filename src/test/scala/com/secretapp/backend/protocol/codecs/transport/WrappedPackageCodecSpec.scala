package com.secretapp.backend.protocol.codecs.transport

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class WrappedPackageCodecSpec extends FlatSpec with Matchers {
  "encode" should "pack Package" in {
    protoWrappedPackage.build(1L, 2L, 3L, RequestAuthId()) should === {
      hex"1e00000000000000010000000000000002000000000000000301f013bb3636".bits.right
    }
  }

  "decode" should "unpack bytes to Package" in {
    val v = hex"1e00000000000000010000000000000002000000000000000301f013bb3636".bits
    val p = WrappedPackage(Package(1L, 2L, ProtoMessageWrapper(3L, RequestAuthId())))
    protoWrappedPackage.decode(v) should === ((BitVector.empty, p).right)
  }

}
