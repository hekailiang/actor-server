package com.secretapp.backend.protocol.codecs

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class StringSpec extends FlatSpec with Matchers {

  val c = StringCodec.string

  "encode" should "pack string" in {
    c.encode("strтестΩ≈ç√") should === ((hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a".bits).right)
    c.encode("") should === ((hex"0".bits).right)
  }

  "decode" should "unpack bytes to string" in {
    val res = c.decode(hex"15737472d182d0b5d181d182cea9e28988c3a7e2889a".bits).toOption.get
    res._1 should === (BitVector.empty)
    res._2 should === ("strтестΩ≈ç√")

    val resWithTail = c.decode(hex"0000000000000000647fffffffffffffff".bits).toOption.get
    resWithTail._1 should === (hex"00000000000000647fffffffffffffff".bits)
    resWithTail._2 should === ("")
  }

}
