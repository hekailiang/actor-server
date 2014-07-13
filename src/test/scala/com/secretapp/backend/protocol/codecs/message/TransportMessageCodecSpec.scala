package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc._
import scodec.bits._
import org.scalatest._
import scalaz._
import Scalaz._

class TransportMessageCodecSpec extends FlatSpec with Matchers {
  "ProtoMessage" should "encode and decode RequestAuth" in {
    val encoded = hex"f0".bits

    protoTransportMessage.encode(RequestAuthId()) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, RequestAuthId()))
    )
  }

  "ProtoMessage" should "encode and decode ResponseAuth" in {
    val encoded = hex"f10000000000000005".bits

    protoTransportMessage.encode(ResponseAuthId(5L)) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, ResponseAuthId(5L)))
    )
  }

  "ProtoMessage" should "encode and decode Ping" in {
    val encoded = hex"010000000000000005".bits

    protoTransportMessage.encode(Ping(5L)) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, Ping(5L)))
    )
  }

  "ProtoMessage" should "encode and decode Pong" in {
    val encoded = hex"020000000000000005".bits

    protoTransportMessage.encode(Pong(5L)) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, Pong(5L)))
    )
  }

  "ProtoMessage" should "encode and decode Drop" in {
    val encoded = hex"0d000000000000000515737472d182d0b5d181d182cea9e28988c3a7e2889a".bits
    val decoded = Drop(5L, "strтестΩ≈ç√")

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  //  Updates

  "ProtoMessage" should "encode and decode Update.Message" in {
    val encoded = hex"0501087b10c803180a20b32b280132030102033a03040506".bits
    val decoded = UpdateBox(Message(123, 456, 10, 5555L, true, Some(List(1, 2, 3)), List(4, 5, 6)))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  "ProtoMessage" should "encode and decode Update.NewDevice" in {
    val encoded = hex"0502087b10e707".bits
    val decoded = UpdateBox(NewDevice(123, 999L))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  "ProtoMessage" should "encode and decode Update.NewYourDevice" in {
    val encoded = hex"0503087b10e7071a03010203".bits
    val decoded = UpdateBox(NewYourDevice(123, 999L, List(1, 2, 3)))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  //  RPC Requests



  //  RPC Responses

  "ProtoMessage" should "encode and decode RpcResponse.CommonUpdate" in {
    val encoded = hex"0400000000000000010d08011203010203186422020506".bits
    val decoded = RpcResponseBox(1L, CommonUpdate(1, List(1, 2, 3), 100, List(5 ,6)))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

}
