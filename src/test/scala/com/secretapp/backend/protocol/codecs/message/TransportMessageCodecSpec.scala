package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.update.{ State => StateU }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct._
import scala.collection.immutable.Seq
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

    "encode and decode Update.Message" in {
      val encoded = hex"051800000001087b10c803180a20b32b28013202ac1d3a021001".bits
      val decoded = UpdateBox(CommonUpdate(1, BitVector.empty, Message(123, 456, 10, 5555L, true, Some(hex"ac1d".bits), hex"1001".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.NewDevice" in {
      val encoded = hex"050900000002087b10e707".bits
      val decoded = UpdateBox(CommonUpdate(1, BitVector.empty, NewDevice(123, 999L)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.NewYourDevice" in {
      val encoded = hex"050d00000003087b10e7071a02ac1d".bits
      val decoded = UpdateBox(CommonUpdate(1, BitVector.empty, NewYourDevice(123, 999L, hex"ac1d".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.CommonUpdate" in {
      val encoded = hex"05140000000d0f08011202ac1d18022205087b10e707".bits
      val decoded = UpdateBox(CommonUpdate(1, hex"ac1d".bits, NewDevice(123, 999L)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode Update.CommonUpdateTooLong" in {
      val encoded = hex"05050000001900".bits
      val decoded = UpdateBox(CommonUpdateTooLong())

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    //  RPC Requests

    "encode and decode RpcRequest.RequestGetDifference" in {
      val encoded = hex"030c010000000b06087b1202ac1d".bits
      val decoded = RpcRequestBox(Request(RequestGetDifference(123, hex"ac1d".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestAuthCode" in {
      val encoded = hex"031b0100000001150888a0a5bda90210b9601a09776f776170696b6579".bits
      val decoded = RpcRequestBox(Request(RequestAuthCode(79853867016L, 12345, "wowapikey")))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestAuthCode with InitConnection" in {
      val encoded = hex"0347022b080110021a056170706c6522086970686f6e6520302a07656e676c6973683207656e676c6973683a02757300000001150888a0a5bda90210b9601a09776f776170696b6579".bits
      val initCon = InitConnection(1, 2, "apple", "iphone 0", "english", "english", "us".some)
      val decoded = RpcRequestBox(RequestWithInit(initCon, RequestAuthCode(79853867016L, 12345, "wowapikey")))

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
      val encoded = hex"032401000000031e0888a0a5bda9021209736d736861736831321a063630353036302202ac1d".bits
      val decoded = RpcRequestBox(Request(RequestSignIn(79853867016L, "smshash12", "605060", hex"ac1d".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestSignUp with valid public key" in {
      val encoded = hex"037c0100000004760888a0a5bda9021209736d736861736831321a06363035303630220754696d6f7468792a044b6c696d324b3049301306072a8648ce3d020106082a8648ce3d03010103320004d547575bae9d648b8f6636cf7c8865d95871dff0575e8538697a4ac06132fce3ec279540e12f14a35fb5ca28e0c37721".bits
      val publicKey = hex"3049301306072a8648ce3d020106082a8648ce3d03010103320004d547575bae9d648b8f6636cf7c8865d95871dff0575e8538697a4ac06132fce3ec279540e12f14a35fb5ca28e0c37721".bits
      val decoded = RpcRequestBox(Request(RequestSignUp(79853867016L, "smshash12", "605060", "Timothy", "Klim".some, publicKey)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcRequest.RequestSignUp with invalid public key" in {
      val decoded = RpcRequestBox(Request(RequestSignUp(79853867016L, "smshash12", "605060", "Timothy", "Klim".some, hex"ac1d".bits)))
      val res = protoTransportMessage.decode(protoTransportMessage.encode(decoded).toOption.get)
      res.toOption should_== None
    }

    "encode and decode RpcRequest.RequestSendMessage" in {
      val encoded = hex"0320010000000e1a08011002180320012a020ae5320c080110021a020ae52202ac1d".bits
      val message = EncryptedMessage(1, 2L, hex"ae5".bits.some, hex"ac1d".bits.some)
      val decoded = RpcRequestBox(Request(RequestSendMessage(1, 2L, 3L, true, hex"ae5".bits.some, Seq(message))))

      protoTransportMessage.encode(decoded) should_== encoded.right
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

    //  RPC Responses

    "encode and decode RpcResponse.Difference" in {
      val encoded = hex"04000000000000000138010000000c3208e7071202ac1d1a1c080110b9601a0754696d6f74687922044b6c696d2802300130023003220908021205087b10e7072800".bits
      val user = User(1, 12345L, "Timothy", Some("Klim"), Some(types.Male), Seq(1L, 2L, 3L))
      val update = DifferenceUpdate(NewDevice(123, 999L))
      val decoded = RpcResponseBox(1L, Ok(Difference(999, hex"ac1d".bits, Seq(user), Seq(update), false)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.State" in {
      val encoded = hex"0400000000000000010c010000000a06087b1202ac1d".bits
      val decoded = RpcResponseBox(1L, Ok(StateU(123, hex"ac1d".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponseAuth" in {
      val encoded = hex"0400000000000000012901000000052308cec2f105121c080110b9601a0754696d6f74687922044b6c696d2802300130023003".bits
      val user = User(1, 12345L, "Timothy", Some("Klim"), Some(types.Male), Seq(1L, 2L, 3L))
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

    "encode and decode RpcResponse.ResponseSendMessage" in {
      val encoded = hex"0400000000000000010e010000000f08080110021a02ac1d".bits
      val decoded = RpcResponseBox(1L, Ok(ResponseSendMessage(1, 2, hex"ac1d".bits)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }

    "encode and decode RpcResponse.ResponseImportedContacts" in {
      val encoded = hex"0400000000000000012b0100000008250a1c080110b9601a0754696d6f74687922044b6c696d280230013002300312050889061001".bits
      val user = User(1, 12345L, "Timothy", Some("Klim"), Some(types.Male), Seq(1L, 2L, 3L))
      val users = Seq(user)
      val contact = ImportedContact(777L, user.uid)
      val contacts = Seq(contact)
      val decoded = RpcResponseBox(1L, Ok(ResponseImportedContacts(users, contacts)))

      protoTransportMessage.encode(decoded) should_== encoded.right
      protoTransportMessage.decode(encoded).toOption should_== (BitVector.empty, decoded).some
    }
  }
}
