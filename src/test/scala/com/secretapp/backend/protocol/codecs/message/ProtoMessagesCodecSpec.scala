package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class ProtoMessagesCodecSpec extends FlatSpec with Matchers {
  "ProtoMessage" should "encode and decode RequestAuth" in {
    val encoded = hex"f0".bits

    protoMessage.encode(RequestAuthId()) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, RequestAuthId()))
    )
  }

  "ProtoMessage" should "encode and decode ResponseAuth" in {
    val encoded = hex"f10000000000000005".bits

    protoMessage.encode(ResponseAuthId(5L)) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, ResponseAuthId(5L)))
    )
  }

  "ProtoMessage" should "encode and decode Ping" in {
    val encoded = hex"010000000000000005".bits

    protoMessage.encode(Ping(5L)) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, Ping(5L)))
    )
  }

  "ProtoMessage" should "encode and decode Pong" in {
    val encoded = hex"020000000000000005".bits

    protoMessage.encode(Pong(5L)) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, Pong(5L)))
    )
  }

  "ProtoMessage" should "encode and decode Drop" in {
    val encoded = hex"0d000000000000000515737472d182d0b5d181d182cea9e28988c3a7e2889a".bits
    val decoded = Drop(5L, "strтестΩ≈ç√")

    protoMessage.encode(decoded) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

}
