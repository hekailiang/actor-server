package com.secretapp.backend.protocol.codecs

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._
import com.secretapp.backend.protocol._

class VarIntSpec extends FlatSpec with Matchers {

  "encode" should "pack VarInt" in {
    varint.encode(150) should === (hex"9601".bits.right)
    varint.encode(300) should === (hex"ac02".bits.right)
    varint.encode(Int.MaxValue) should === (hex"ffffffff07".bits.right)
    varint.encode(Int.MinValue + 1) should === (hex"ffffffff07".bits.right)
  }

  "decode" should "unpack bytes to VarInt" in {
    varint.decode(hex"9601".bits) should === ((BitVector.empty, 150).right)
    varint.decode(hex"9601".bits) should === ((BitVector.empty, 150).right)
    varint.decode(hex"ac02".bits) should === ((BitVector.empty, 300).right)
    varint.decode(hex"ffffffff07".bits) should === ((BitVector.empty, Int.MaxValue).right)
    varint.decode(hex"9601ff".bits) should === ((hex"ff".bits, 150).right)
  }

}
