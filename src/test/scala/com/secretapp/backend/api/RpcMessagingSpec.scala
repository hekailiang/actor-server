package com.secretapp.backend.api

import akka.actor._
import akka.testkit._
import com.gilt.timeuuid.TimeUuid
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.{update => updateProto, _}
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.struct
import scala.collection.immutable
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist.CassandraSpecification
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import scodec.codecs.uuid
import scala.concurrent.duration._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.utils.protobuf._
import org.specs2.mutable.ActorLikeSpecification
import com.newzly.util.testing.AsyncAssertionsHelper._
import akka.io.Tcp._
import akka.util.ByteString
import scala.util.Random
import scodec.bits._
import scalaz._
import Scalaz._

class RpcMessagingSpec extends ActorLikeSpecification with CassandraSpecification
{
  import system.dispatcher

  override lazy val actorSystemName = "api"

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandlerActor(probe.ref, session) {
      override lazy val rand = new Random() {
        override def nextLong() = 12345L
      }
    }))
    (probe, actor)
  }

  val rand = new Random()

  "RpcMessaging" should {
    "reply to SendMessage and push to sequence" in {
      val (probe, apiActor) = probeAndActor()

      // Sign in
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      val messageId = rand.nextLong()
      val phone = 79853867016L
      val pubKey = hex"ac1d".bits
      val pubkeyHash = 1L
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val userId = 1090901

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phone, "wow", "such sms", pubKey)))
      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, "wow", "such sms")).sync()
      UserRecord.insertEntity(User.build(userId, pubKey, firstName, lastName, NoSex, pubkeyHash))
      PhoneRecord.insertEntity(Phone(phone, userId))

      probe.send(apiActor, Received(ByteString(buf)))

      val user = struct.User(userId, 1L, "Timothy", Some("Klim"), None, immutable.Seq(1L))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
      val expectMsgs = immutable.Seq(ackRes, res)
      probe.expectMsgAllOf(expectMsgs :_*)

      // insert second user
      val sndpubkeyHash = 3L
      UserRecord.insertEntity(
        User.build(3000, hex"ac1d3000".bits, "Wayne", Some("Brain"), NoSex, sndpubkeyHash)
      ).sync()

      {
        val rq = RequestSendMessage(
          uid = userId, accessHash = 0L,
          randomId = 555L, useAesKey = false,
          aesMessage = None,
          messages = immutable.Seq(
            EncryptedMessage(uid = 3000, keyHash = sndpubkeyHash, None, Some(BitVector(1,2,3)))
          )
        )
        val mesageId = rand.nextLong()
        val rpcRq = RpcRequestBox(Request(rq))
        val p = Package(authId, sessionId, MessageBox(messageId, rpcRq))

        val encoded = protoPackageBox.encode(p)
        val buf = encoded.toOption.get.toByteBuffer

        probe.send(apiActor, Received(ByteString(buf)))

        val state = TimeUuid()
        val rsp = ResponseSendMessage(mid = 1, seq = 1, state = uuid.encode(state).toOption.get)
        val rpcRsp = RpcResponseBox(
          messageId,
          Ok(rsp))
        val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
        val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
        val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
        val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
        val expectMsgs = immutable.Seq(ackRes, res)

        probe.expectMsgAllOf(new DurationInt(7).seconds, expectMsgs :_*)
      }

      success
    }
  }
}
