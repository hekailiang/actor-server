package com.secretapp.backend.protocol.codecs

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._
import com.secretapp.backend.protocol._

class BytesSpec extends FlatSpec with Matchers {

  "encode" should "pack ByteVector" in {
    val v = hex"f0aff01".bits
    bytes.encode(v) should === ((hex"4".bits ++ v).right)
  }

  "decode" should "unpack bytes to ByteVector" in {
    val v = hex"f0aff01".bits
    val res = bytes.decode(hex"4".bits ++ v).toOption.get
    res._1 should === (BitVector.empty)
    res._2 should === (v)
  }

}
