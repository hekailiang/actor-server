package com.secretapp.backend.protocol.codecs

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class LongsSpec extends FlatSpec with Matchers {

  "encode" should "pack array of longs" in {
    Longs.encode(Array(100L, Long.MaxValue)) should === (hex"0200000000000000647fffffffffffffff".bits.right)
    Longs.encode(Array[Long]()) should === (hex"0".bits.right)
  }

  "decode" should "unpack bytes to array of longs" in {
    val res = Longs.decode(hex"0200000000000000647fffffffffffffff".bits).toOption.get
    res._1 should === (BitVector.empty)
    res._2 should === (Array(100L, Long.MaxValue))

    val resWithTail = Longs.decode(hex"0000000000000000647fffffffffffffff".bits).toOption.get
    resWithTail._1 should === (hex"00000000000000647fffffffffffffff".bits)
    resWithTail._2 should === (Array[Long]())
  }

}
