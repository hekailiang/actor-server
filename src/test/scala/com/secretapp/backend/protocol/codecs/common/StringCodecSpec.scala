package com.secretapp.backend.protocol.codecs.common

import com.secretapp.backend.protocol.codecs._
import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class StringCodecSpec extends FlatSpec with Matchers {

  "encode" should "pack string" in {
    protoString.encode("strтестΩ≈ç√") should === ((hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a".bits).right)
    protoString.encode("") should === ((hex"0".bits).right)
  }

  "decode" should "unpack bytes to string" in {
    val res = protoString.decode(hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a".bits).toOption.get
    res._1 should === (BitVector.empty)
    res._2 should === ("strтестΩ≈ç√")

    val resWithTail = protoString.decode(hex"0000000000000000647fffffffffffffff".bits).toOption.get
    resWithTail._1 should === (hex"00000000000000647fffffffffffffff".bits)
    resWithTail._2 should === ("")
  }

}
