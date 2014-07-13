package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.struct._
import scala.collection.immutable.Seq
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

  "ProtoMessage" should "encode and decode RpcRequest.RequestAuthCode" in {
    val encoded = hex"03010888a0a5bda90210b9601a09776f776170696b6579".bits
    val decoded = RpcRequestBox(RequestAuthCode(79853867016L, 12345, "wowapikey"))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  "ProtoMessage" should "encode and decode RpcRequest.RequestSignIn" in {
    val encoded = hex"03030888a0a5bda9021209736d736861736831321a063630353036302203010203".bits
    val decoded = RpcRequestBox(RequestSignIn(79853867016L, "smshash12", "605060", List(1, 2, 3)))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  "ProtoMessage" should "encode and decode RpcRequest.RequestSignUp" in {
    val encoded = hex"03040888a0a5bda9021209736d736861736831321a06363035303630220754696d6f7468792a044b6c696d3203010203".bits
    val decoded = RpcRequestBox(RequestSignUp(79853867016L, "smshash12", "605060", "Timothy", "Klim".some, List(1, 2, 3)))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  //  RPC Responses

  "ProtoMessage" should "encode and decode RpcResponse.CommonUpdate" in {
    val encoded = hex"0400000000000000010d08011203010203186422020506".bits
    val decoded = RpcResponseBox(1L, CommonUpdate(1, List(1, 2, 3), 100, List(5 ,6)))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  "ProtoMessage" should "encode and decode RpcResponse.ResponseAuth" in {
    val encoded = hex"0400000000000000010508cec2f105121c080110b9601a0754696d6f74687922044b6c696d2802300130023003".bits
    val user = User(1, 12345L, "Timothy", Some("Klim"), Some(types.Male), Seq(1L, 2L, 3L))
    val decoded = RpcResponseBox(1L, ResponseAuth(12345678L, user))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

  "ProtoMessage" should "encode and decode RpcResponse.ResponseAuthCode" in {
    val encoded = hex"040000000000000001020a07736d73686173681001".bits
    val decoded = RpcResponseBox(1L, ResponseAuthCode("smshash", true))

    protoTransportMessage.encode(decoded) should === (encoded.right)
    protoTransportMessage.decode(encoded).toOption should === (
      Some((BitVector.empty, decoded))
    )
  }

}
