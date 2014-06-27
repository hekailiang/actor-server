package com.secretapp.backend.protocol.types

import scodec.bits._
import org.scalatest._

class StringSpec extends FlatSpec with Matchers {

  "encode" should "pack string" in {
    String.encode("strтестΩ≈ç√") should === (hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a")
    String.encode("") should === (hex"0")
  }

  "take" should "unpack bytes to string" in {
    val res = String.take(hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a")
    res._1 should === ("strтестΩ≈ç√")
    res._2 should === (ByteVector.empty)

    val resWithTail = String.take(hex"0000000000000000647fffffffffffffff")
    resWithTail._1 should === ("")
    resWithTail._2 should === (hex"00000000000000647fffffffffffffff")
  }

}
