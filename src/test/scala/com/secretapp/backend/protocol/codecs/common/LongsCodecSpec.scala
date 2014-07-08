package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class LongsCodecSpec extends FlatSpec with Matchers {

  "encode" should "pack array of longs" in {
    protoLongs.encode(Array(100L, Long.MaxValue)) should === (hex"0200000000000000647fffffffffffffff".bits.right)
    protoLongs.encode(Array[Long]()) should === (hex"0".bits.right)
  }

  "decode" should "unpack bytes to array of longs" in {
    val res = protoLongs.decode(hex"0200000000000000647fffffffffffffff".bits).toOption.get
    res._1 should === (BitVector.empty)
    res._2 should === (Array(100L, Long.MaxValue))

    val resWithTail = protoLongs.decode(hex"0000000000000000647fffffffffffffff".bits).toOption.get
    resWithTail._1 should === (hex"00000000000000647fffffffffffffff".bits)
    resWithTail._2 should === (Array[Long]())
  }

}
