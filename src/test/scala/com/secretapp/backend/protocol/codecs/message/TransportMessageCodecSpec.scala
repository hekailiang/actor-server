package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct._
import java.util.UUID
import scala.collection.immutable
import scodec.bits._
import org.specs2.mutable.Specification
import scalaz._
import Scalaz._

class TransportMessageCodecSpec extends Specification {
  "TransportMessageCodec" should {
    "encode and decode RequestAuth" in {
      val encoded = hex"f0".bits

      protoTransportMessage.encode(RequestAuthId()) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, RequestAuthId()).some
    }

    "encode and decode ResponseAuth" in {
      val encoded = hex"f10000000000000005".bits

      protoTransportMessage.encode(ResponseAuthId(5L)) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, ResponseAuthId(5L)).some
    }

    "encode and decode Ping" in {
      val encoded = hex"010000000000000005".bits

      protoTransportMessage.encode(Ping(5L)) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, Ping(5L)).some
    }

    "encode and decode Pong" in {
      val encoded = hex"020000000000000005".bits

      protoTransportMessage.encode(Pong(5L)) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, Pong(5L)).some
    }

    "encode and decode Drop" in {
      val encoded = hex"0d000000000000000515737472d182d0b5d181d182cea9e28988c3a7e2889a".bits
      val decoded = Drop(5L, "strтестΩ≈ç√")

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode UnsentMessage" in {
      val encoded = hex"0700000000000000050000007b".bits
      val decoded = UnsentMessage(5L, 123)

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode UnsentResponse" in {
      val encoded = hex"08000000000000000100000000000000050000007b".bits
      val decoded = UnsentResponse(1L, 5L, 123)

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RequestResend" in {
      val encoded = hex"090000000000000001".bits
      val decoded = RequestResend(1L)

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    //  Updates

    "encode and decode Update.NewDevice" in {
      val encoded = hex"05120000000d0d0801120018022205087b10e707".bits
      val decoded = UpdateBox(SeqUpdate(1, BitVector.empty, NewDevice(123, 999L)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.NewYourDevice" in {
      val encoded = hex"05160000000d110801120018032209087b10e7071a02ac1d".bits
      val decoded = UpdateBox(SeqUpdate(1, BitVector.empty, NewYourDevice(123, 999L, hex"ac1d".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.SeqUpdate" in {
      val encoded = hex"05140000000d0f08011202ac1d18022205087b10e707".bits
      val decoded = UpdateBox(SeqUpdate(1, hex"ac1d".bits, NewDevice(123, 999L)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.SeqUpdateTooLong" in {
      val encoded = hex"05050000001900".bits
      val decoded = UpdateBox(SeqUpdateTooLong())

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    //  RPC Requests

    "describe error for invalid data in RpcRequest" in {
      val encoded = hex"030cff".bits
      protoTransportMessage.decode(encoded) should_== "RpcRequestBox.type is unknown. Body: '', length: 0".left
    }

    "encode and decode RpcRequest.RequestGetDifference" in {
      val encoded = hex"031a010000000b14087b1210c62a5342b7624d6586138ccbb48dfa69".bits
      val decoded = RpcRequestBox(Request(RequestGetDifference(123, Some(UUID.fromString("c62a5342-b762-4d65-8613-8ccbb48dfa69")))))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestAuthCode" in {
      val encoded = hex"031b0100000001150888a0a5bda90210b9601a09776f776170696b6579".bits
      val decoded = RpcRequestBox(Request(RequestAuthCode(79853867016L, 12345, "wowapikey")))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestGetState" in {
      val encoded = hex"0306010000000900".bits
      val decoded = RpcRequestBox(Request(RequestGetState()))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestSignIn" in {
      val encoded = hex"033a0100000003340888a0a5bda9021209736d736861736831321a0636303530363022182db8570cb1ac92036d39fb02f0438856ff91edccde4642f4".bits
      val publicKey = hex"2db8570cb1ac92036d39fb02f0438856ff91edccde4642f4".bits
      val decoded = RpcRequestBox(Request(RequestSignIn(79853867016L, "smshash12", "605060", publicKey)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestSignUp with valid public key" in {
      val encoded = hex"03480100000004420888a0a5bda9021209736d736861736831321a06363035303630220c54696d6f746879204b6c696d32182db8570cb1ac92036d39fb02f0438856ff91edccde4642f4".bits
      val publicKey = hex"2db8570cb1ac92036d39fb02f0438856ff91edccde4642f4".bits
      val decoded = RpcRequestBox(Request(RequestSignUp(79853867016L, "smshash12", "605060", "Timothy Klim", publicKey)))

      protoTransportMessage.encode(decoded).toOption.get should_== encoded
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestImportContacts" in {
      val encoded = hex"03270100000007210a0908011089a0a5bda9020a090802108aa0a5bda9020a090803108ba0a5bda902".bits
      val contacts = (1L to 3L).map { id =>
        ContactToImport(id, 79853867016L + id)
      }
      val decoded = RpcRequestBox(Request(RequestImportContacts(contacts)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestPublicKeys" in {
      val encoded = hex"032a0100000006240a0a08011088ad4b18b3f6780a0a08021089ad4b18b4f6780a0a0803108aad4b18b5f678".bits
      val keys = (1 to 3).map { id =>
        PublicKeyRequest(id, 1234567L + id, 1981234L + id)
      }
      val decoded = RpcRequestBox(Request(RequestPublicKeys(keys)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    //  RPC Responses

    "encode and decode RpcResponse.Difference" in {
      val encoded = hex"0400000000000000014c010000000c4608e7071210c62a5342b7624d6586138ccbb48dfa691a22080110b9601a0c54696d6f746879204b6c696d28023001300230033888a0a5bda902220908021205087b10e7072800".bits
      val user = User(1, 12345L, "Timothy Klim", Some(types.Male), Set(1L, 2L, 3L), 79853867016L)
      val update = DifferenceUpdate(NewDevice(123, 999L))
      val decoded = RpcResponseBox(1L, Ok(Difference(999, Some(UUID.fromString("c62a5342-b762-4d65-8613-8ccbb48dfa69")), immutable.Seq(user), immutable.Seq(update), false)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponseSeq" in {
      val encoded = hex"0400000000000000011a010000004814087b1210c62a5342b7624d6586138ccbb48dfa69".bits
      val decoded = RpcResponseBox(1L, Ok(ResponseSeq(123, Some(UUID.fromString("c62a5342-b762-4d65-8613-8ccbb48dfa69")))))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponseAuth" in {
      val encoded = hex"0400000000000000012f01000000052908cec2f1051222080110b9601a0c54696d6f746879204b6c696d28023001300230033888a0a5bda902".bits
      val user = User(1, 12345L, "Timothy Klim", Some(types.Male), Set(1L, 2L, 3L), 79853867016L)
      val decoded = RpcResponseBox(1L, Ok(ResponseAuth(12345678L, user)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponseAuthCode" in {
      val encoded = hex"0400000000000000011101000000020b0a07736d73686173681001".bits
      val decoded = RpcResponseBox(1L, Ok(ResponseAuthCode("smshash", true)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.Error" in {
      val encoded = hex"0400000000000000012302000000010b3430305f554e4b4e4f574e0f776f772c2073756368206572726f720100".bits
      val decoded = RpcResponseBox(1L, Error(1, "400_UNKNOWN", "wow, such error", true))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ConnectionNotInitedError" in {
      val encoded = hex"0400000000000000010105".bits
      val decoded = RpcResponseBox(1L, ConnectionNotInitedError())

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.FloodWait" in {
      val encoded = hex"04000000000000000105030000007b".bits
      val decoded = RpcResponseBox(1L, FloodWait(123))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.InternalError" in {
      val encoded = hex"04000000000000000106040100000063".bits
      val decoded = RpcResponseBox(1L, InternalError(true, 99))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponseImportedContacts" in {
      val encoded = hex"0400000000000000013101000000082b0a22080110b9601a0c54696d6f746879204b6c696d28023001300230033888a0a5bda90212050889061001".bits
      val user = User(1, 12345L, "Timothy Klim", Some(types.Male), Set(1L, 2L, 3L), 79853867016L)
      val users = immutable.Seq(user)
      val contact = ImportedContact(777L, user.uid)
      val contacts = immutable.Seq(contact)
      val decoded = RpcResponseBox(1L, Ok(ResponseImportedContacts(users, contacts)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponsePublicKeys" in {
      val encoded = hex"0400000000000000013301000000182d0a0d08011088ad4b1a0561633164310a0d08021089ad4b1a0561633164320a0d0803108aad4b1a056163316433".bits
      val keys = (1 to 3).map { id =>
        PublicKeyResponse(id, 1234567L + id, BitVector(s"ac1d${id}".getBytes))
      }
      val decoded = RpcResponseBox(1L, Ok(ResponsePublicKeys(keys)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }
  }
}
