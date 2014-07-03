package com.secretapp.backend.protocol.codecs

import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._
import com.secretapp.backend.data._
import com.secretapp.backend.protocol._

class MessagesSpec extends FlatSpec with Matchers {
  "Message" should "encode and decode RequestAuth" in {
    val encoded = hex"f0".bits

    protoMessage.encode(RequestAuthId()) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, RequestAuthId()))
    )
  }

  "Message" should "encode and decode ResponseAuth" in {
    val encoded = hex"f10000000000000005".bits

    protoMessage.encode(ResponseAuthId(5L)) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, ResponseAuthId(5L)))
    )
  }

  "Message" should "encode and decode Ping" in {
    val encoded = hex"010000000000000005".bits

    protoMessage.encode(Ping(5L)) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, Ping(5L)))
    )
  }

  "Message" should "encode and decode Pong" in {
    val encoded = hex"020000000000000005".bits

    protoMessage.encode(Pong(5L)) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, Pong(5L)))
    )
  }

  "Message" should "encode and decode Drop" in {
    val encoded = hex"0d000000000000000515737472d182d0b5d181d182cea9e28988c3a7e2889a".bits
    val decoded = Drop(5L, "strтестΩ≈ç√")

    protoMessage.encode(decoded) should === (encoded.right)
    protoMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }
}