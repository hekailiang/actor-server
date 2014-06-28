//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits._
//import org.scalatest._
//
//class BytesSpec extends FlatSpec with Matchers {
//
//  "encode" should "pack ByteVector" in {
//    val v = hex"f0aff01".bits
//    Bytes.encode(v) should === (hex"4".bits ++ v)
//  }
//
//  "take" should "unpack bytes to ByteVector" in {
//    val v = hex"f0aff01".bits
//    val res = Bytes.take(hex"4".bits ++ v)
//    res._1 should === (v)
//    res._2 should === (BitVector.empty)
//  }
//
//}
