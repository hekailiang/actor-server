//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits._
//import org.scalatest._
//
//class LongsSpec extends FlatSpec with Matchers {
//
//  "encodeL" should "pack array of longs" in {
//    Longs.encodeL(Array(100L, Long.MaxValue)) should === (hex"0200000000000000647fffffffffffffff".bits)
//    Longs.encodeL(Array[Long]()) should === (hex"0".bits)
//  }
//
//  "take" should "unpack bytes to array of longs" in {
//    val res = Longs.take(hex"0200000000000000647fffffffffffffff".bits)
//    res._1 should === (Array(100L, Long.MaxValue))
//    res._2 should === (BitVector.empty)
//
//    val resWithTail = Longs.take(hex"0000000000000000647fffffffffffffff".bits)
//    resWithTail._1 should === (Array[Long]())
//    resWithTail._2 should === (hex"00000000000000647fffffffffffffff".bits)
//  }
//
//}
