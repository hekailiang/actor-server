package com.secretapp.backend.api

import scala.language.postfixOps
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import akka.testkit._
import akka.actor.{ Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import org.specs2.mutable.ActorLikeSpecification
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
import java.util.concurrent.atomic.AtomicLong

class ApiHandlerActorSpec extends ActorLikeSpecification with CassandraSpecification
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

  "actor" should {
    "reply with auth token to auth request" in {
      val (probe, apiActor) = probeAndActor()
      val req = protoPackageBox.build(0L, 0L, 1L, RequestAuthId())
      val res = protoPackageBox.build(0L, 0L, 1L, ResponseAuthId(12345L))
      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))
      success
    }

    "reply pong to ping" in {
//      TODO: DRY
      val (probe, apiActor) = probeAndActor()
      val authId = 12345L // TODO: mocks!
      val messageId = new AtomicLong(1L)
      val req = protoPackageBox.build(0L, 0L, messageId.get(), RequestAuthId())
      val res = protoPackageBox.build(0L, 0L, messageId.get(), ResponseAuthId(authId))
      probe.send(apiActor, Received(codecRes2BS(req)))
      probe.expectMsg(Write(codecRes2BS(res)))

      val pingVal = rand.nextLong()
      val sessionId = rand.nextLong()
      val ping = protoPackageBox.build(authId, sessionId, messageId.incrementAndGet(), Ping(pingVal))
      probe.send(apiActor, Received(codecRes2BS(ping)))

      val newNewSession = protoPackageBox.build(authId, sessionId, messageId.get(), NewSession(sessionId, messageId.get()))
      val pong = protoPackageBox.build(authId, sessionId, messageId.get(), Pong(pingVal))
      val ack = protoPackageBox.build(authId, sessionId, messageId.get(), MessageAck(Array(messageId.get())))
      val expectedMsg = Seq(newNewSession, pong, ack)
      probe.expectMsgAllOf(expectedMsg.map(m => Write(codecRes2BS(m))) :_*)
      success
    }

    "send drop to invalid crc" in {
      val (probe, apiActor) = probeAndActor()
      val req = hex"1e00000000000000010000000000000002000000000000000301f013bb3411".bits.toByteBuffer
      val res = protoPackageBox.build(0L, 0L, 0L, Drop(0L, "invalid crc32")).toOption.get.toByteBuffer
      probe.send(apiActor, Received(ByteString(req)))
      probe.expectMsg(Write(ByteString(res)))
      success
    }

    "parse packages in single stream" in { // TODO: replace by scalacheck
      val (probe, apiActor) = probeAndActor()
      val authId = rand.nextLong()
      val sessionId = rand.nextLong()
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()

      val messages: Seq[MessageBox] = (1 to 100).map { _ =>
        MessageBox(rand.nextLong, Ping(rand.nextLong))
      }
      val packages = messages.map(m => codecRes2BS(protoPackageBox.encode(Package(authId, sessionId, m))))
      val req = packages.foldLeft(ByteString.empty)(_ ++ _)
      req.grouped(7) foreach { buf =>
        probe.send(apiActor, Received(buf))
      }

      val expectedPongs = messages flatMap { message =>
        val messageId = message.messageId
        val pingVal = message.body.asInstanceOf[Ping].randomId
        val p = Package(authId, sessionId, MessageBox(messageId, Pong(pingVal)))
        val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
        Seq(codecRes2BS(protoPackageBox.encode(p)), codecRes2BS(protoPackageBox.encode(ack)))
      }
      probe.expectMsgAllOf(expectedPongs.map(Write(_)) :_*)
      success
    }

    "handle container with Ping's" in {
      val (probe, apiActor) = probeAndActor()
      val authId = rand.nextLong
      val sessionId = rand.nextLong
      val messages: Seq[MessageBox] = (1 to 100).map { _ =>
        MessageBox(rand.nextLong, Ping(rand.nextLong))
      }
      val container = MessageBox(rand.nextLong, Container(messages))
      val p = Package(authId, sessionId, container)
      val packageBlob = Received(codecRes2BS(protoPackageBox.encode(p)))
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()

      probe.send(apiActor, packageBlob)

      val expectedPongs = messages flatMap { message =>
        val messageId = message.messageId
        val pingVal = message.body.asInstanceOf[Ping].randomId
        val p = Package(authId, sessionId, MessageBox(messageId, Pong(pingVal)))
        val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
        Seq(codecRes2BS(protoPackageBox.encode(p)), codecRes2BS(protoPackageBox.encode(ack)))
      }
      probe.expectMsgAllOf(expectedPongs.map(Write(_)) :_*)
      success
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
      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
      val expectMsgs = Seq(ackRes, res)
      probe.expectMsgAllOf(expectMsgs :_*)
      success
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
      success
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
      success
    }
  }
}
