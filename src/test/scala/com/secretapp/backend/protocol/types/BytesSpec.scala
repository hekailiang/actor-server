package com.secretapp.backend.protocol.types

import scodec.bits._
import org.scalatest._

class BytesSpec extends FlatSpec with Matchers {

  "encode" should "pack ByteVector" in {
    val v = hex"f0aff01"
    Bytes.encode(v) should === (hex"4" ++ v)
  }

  "take" should "unpack bytes to ByteVector" in {
    val v = hex"f0aff01"
    val res = Bytes.take(hex"4" ++ v)
    res._1 should === (v)
    res._2 should === (ByteVector.empty)
  }

}
