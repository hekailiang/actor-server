package com.secretapp.backend.api

import org.specs2.mutable.ActorSpecification

import scala.language.postfixOps
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import akka.testkit._
import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport.{MessageBox, Package}
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.persist._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.types._
import com.newzly.util.testing.AsyncAssertionsHelper._

class ApiHandlerSpecification extends ActorSpecification with CassandraSpecification
{

  import system.dispatcher

  def probeAndActor() = {
    val probe = TestProbe()
    val session = DBConnector.session
    val actor = system.actorOf(Props(new ApiHandler(probe.ref, session) {
      override lazy val rand = new Random() {
        override def nextLong() = 12345L
      }
    }))
    (probe, actor)
  }

  val rand = new Random()

  "actor" should {
    "reply with auth token to auth request" in {
      val (probe, apiActor) = probeAndActor()
      val req = protoPackageBox.build(0L, 0L, 1L, RequestAuthId())
      val res = protoPackageBox.build(0L, 0L, 1L, ResponseAuthId(12345L))
      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))
    }

    "reply pong to ping" in {
//      TODO: DRY
      val (probe, apiActor) = probeAndActor()
      val authId = 12345L
      val messageId = 1L
      val req = protoPackageBox.build(0L, 0L, messageId, RequestAuthId())
      val res = protoPackageBox.build(0L, 0L, messageId, ResponseAuthId(authId))
      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))

      val randId = 987654321L
      val sessionId = 123L
      val ping = protoPackageBox.build(authId, sessionId, messageId + 1, Ping(randId))
      val newNewSession = protoPackageBox.build(authId, sessionId, messageId + 1, NewSession(sessionId, messageId + 1))
      val pong = protoPackageBox.build(authId, sessionId, messageId + 1, Pong(randId))

      probe.send(apiActor, Received(ByteString(ping.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(newNewSession.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(pong.toOption.get.toByteBuffer)))
    }

    "send drop to invalid crc" in {
      val (probe, apiActor) = probeAndActor()
      val req = hex"1e00000000000000010000000000000002000000000000000301f013bb3411".bits.toByteBuffer
      val res = protoPackageBox.build(0L, 0L, 0L, Drop(0L, "invalid crc32")).toOption.get.toByteBuffer
      probe.send(apiActor, Received(ByteString(req)))
      probe.expectMsg(Write(ByteString(res)))
    }

    "parse packages in single stream" in { // TODO: replace by scalacheck
      val (probe, apiActor) = probeAndActor()
      val ids = (1L to 100L) map ((_, rand.nextLong))
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()

      val res = ids.map { (item) =>
        val (msgId, pingVal) = item
        val p = Package(authId, sessionId, MessageBox(msgId, Ping(pingVal)))
        protoPackageBox.encode(p).toOption.get
      }.foldLeft(BitVector.empty)(_ ++ _).toByteBuffer
      ByteString(res).grouped(7) foreach { buf =>
        probe.send(apiActor, Received(buf))
      }

      val expectedPongs = ids map { (item) =>
        val (msgId, pingVal) = item
        val p = Package(authId, sessionId, MessageBox(msgId, Pong(pingVal)))
        val res = ByteString(protoPackageBox.encode(p).toOption.get.toByteBuffer)
        Write(res)
      }
      probe.expectMsgAllOf(expectedPongs :_*)
    }

    "handle container with Ping's" in {
      val (probe, apiActor) = probeAndActor()
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      val messageId = rand.nextLong()
      val pingVal = 5L
      val messageBoxId = 1L
      val ping = MessageBox(messageBoxId, Ping(pingVal))
      val container = Container(Seq(ping, ping, ping))
      val p = Package(authId, sessionId, MessageBox(messageId, container))
      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()

      probe.send(apiActor, Received(ByteString(buf)))

      val expectedPongs = (1 to container.messages.length) map { id =>
        val p = Package(authId, sessionId, MessageBox(messageBoxId, Pong(pingVal)))
        val res = ByteString(protoPackageBox.encode(p).toOption.get.toByteBuffer)
        Write(res)
      }
      probe.expectMsgAllOf(expectedPongs :_*)
    }

    "handle RPC request auth code" in {
      val (probe, apiActor) = probeAndActor()
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      val messageId = rand.nextLong()
      val rpcReq = RpcRequestBox(Request(RequestAuthCode(79853867016L, 123, "apikey")))
      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()

      probe.send(apiActor, Received(ByteString(buf)))

      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuthCode("12345", false)))
      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
      println(ack, protoPackageBox.encode(ack))
      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
      val expectMsgs = Seq(ackRes, res)
      probe.expectMsgAllOf(expectMsgs :_*)
    }

    "handle RPC request sign up" in {
      val (probe, apiActor) = probeAndActor()
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      val messageId = rand.nextLong()
      val phone = 79853867016L

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phone, "wow", "such sms", "Timothy", Some("Klim"), hex"ac1d".bits)))
      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, "wow", "such sms")).sync()

      probe.send(apiActor, Received(ByteString(buf)))

      val user = struct.User(1090901, 1, "Timothy", Some("Klim"), None, Seq(1L))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
      val expectMsgs = Seq(ackRes, res)
      probe.expectMsgAllOf(expectMsgs :_*)
    }

    "handle RPC request sign in" in {
      val (probe, apiActor) = probeAndActor()
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      val messageId = rand.nextLong()
      val phone = 79853867016L
      val pubKey = hex"ac1d".bits
      val firstName = "Timothy"
      val lastName = Some("Klim")
      val userId = 1090901

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phone, "wow", "such sms", pubKey)))
      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, "wow", "such sms")).sync()
      UserRecord.insertEntity(Entity(userId, User.build(pubKey, firstName, lastName, NoSex)))
      PhoneRecord.insertEntity(Phone(phone, userId))

      probe.send(apiActor, Received(ByteString(buf)))

      val user = struct.User(userId, 1L, "Timothy", Some("Klim"), None, Seq(1L))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
      val expectMsgs = Seq(ackRes, res)
      probe.expectMsgAllOf(expectMsgs :_*)
    }
  }
}
