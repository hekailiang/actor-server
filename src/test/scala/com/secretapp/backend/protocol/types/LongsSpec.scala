package com.secretapp.backend.protocol.types

import scodec.bits._
import org.scalatest._

class LongsSpec extends FlatSpec with Matchers {

  "encodeL" should "pack array of longs" in {
    Longs.encodeL(Array(100L, Long.MaxValue)) should === (hex"0200000000000000647fffffffffffffff")
    Longs.encodeL(Array[Long]()) should === (hex"0")
  }

  "take" should "unpack bytes to array of longs" in {
    val res = Longs.take(hex"0200000000000000647fffffffffffffff")
    res._1 should === (Array(100L, Long.MaxValue))
    res._2 should === (ByteVector.empty)

    val resWithTail = Longs.take(hex"0000000000000000647fffffffffffffff")
    resWithTail._1 should === (Array[Long]())
    resWithTail._2 should === (hex"00000000000000647fffffffffffffff")
  }

}
