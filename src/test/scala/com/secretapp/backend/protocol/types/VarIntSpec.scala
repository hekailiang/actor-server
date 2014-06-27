package com.secretapp.backend.protocol.types

import scodec.bits._
import org.scalatest._

class VarIntSpec extends FlatSpec with Matchers {

  "encode" should "pack Int" in {
    VarInt.encode(150) should === (hex"9601")
    VarInt.encode(300) should === (hex"ac02")
    VarInt.encode(Int.MaxValue) should === (hex"ffffffff07")
    VarInt.encode(Int.MinValue + 1) should === (hex"ffffffff07")
  }

  "decode" should "unpack bytes to Int" in {
    VarInt.decode(hex"9601") should === (150)
    VarInt.decode(hex"ac02") should === (300)
    VarInt.decode(hex"ffffffff07") should === (Int.MaxValue)
  }

  "take" should "unpack bytes to Int" in {
    VarInt.take(hex"9601")._1 should === (150)
    VarInt.take(hex"ac02")._1 should === (300)
    VarInt.take(hex"ffffffff07")._1 should === (Int.MaxValue)
    val res = VarInt.take(hex"9601ff")
    res._1 should === (150)
    res._2 should === (hex"ff")
  }

  "varIntLen" should "count size of VarInt" in {
    VarInt.varIntLen(hex"9601") should === (2)
    VarInt.varIntLen(hex"ac02") should === (2)
    VarInt.varIntLen(hex"ffffffff07") should === (5)
    VarInt.varIntLen(hex"ac0201020304") should === (2)
    VarInt.varIntLen(hex"0201020304") should === (1)
  }

}
