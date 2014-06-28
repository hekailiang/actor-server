package com.secretapp.backend.protocol.codecs

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class VarIntSpec extends FlatSpec with Matchers {

  val c = VarInt.codec

  "encode" should "pack VarInt" in {
    c.encode(150) should === (hex"9601".bits.right)
    c.encode(300) should === (hex"ac02".bits.right)
    c.encode(Int.MaxValue) should === (hex"ffffffff07".bits.right)
    c.encode(Int.MinValue + 1) should === (hex"ffffffff07".bits.right)
  }

  "decode" should "unpack bytes to VarInt" in {
    c.decode(hex"9601".bits) should === ((BitVector.empty, 150).right)
    c.decode(hex"9601".bits) should === ((BitVector.empty, 150).right)
    c.decode(hex"ac02".bits) should === ((BitVector.empty, 300).right)
    c.decode(hex"ffffffff07".bits) should === ((BitVector.empty, Int.MaxValue).right)
    c.decode(hex"9601ff".bits) should === ((hex"ff".bits, 150).right)
  }

}
