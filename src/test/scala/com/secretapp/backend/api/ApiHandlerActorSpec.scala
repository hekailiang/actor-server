package com.secretapp.backend.api

import scala.language.{ postfixOps, higherKinds }
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import akka.testkit._
import akka.actor.{ Props, Actor }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import org.specs2.mutable.{ActorServiceHelpers, ActorLikeSpecification}
import org.scalamock.specs2.MockFactory
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
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.newzly.util.testing.AsyncAssertionsHelper._
import java.util.concurrent.atomic.AtomicLong

class ApiHandlerActorSpec extends ActorLikeSpecification with CassandraSpecification with MockFactory with ActorServiceHelpers
{
  import system.dispatcher

  override lazy val actorSystemName = "api"

  trait RandomServiceMock extends RandomService { self: Actor =>
    override lazy val rand = mock[Random]

    override def preStart(): Unit = {
      withExpectations {
        (rand.nextLong _) stubs() returning(12345L)
      }
    }
  }

  trait GeneratorServiceMock extends GeneratorService {
    override def genNewAuthId = mockAuthId
  }

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandlerActor(probe.ref, session) with RandomServiceMock with GeneratorServiceMock))
    (probe, actor)
  }

  "actor" should {
    "reply with auth token to auth request" in {
      val (probe, apiActor) = probeAndActor()
      val messageId = rand.nextLong()
      val req = protoPackageBox.build(0L, 0L, messageId, RequestAuthId())
      val res = protoPackageBox.build(0L, 0L, messageId, ResponseAuthId(mockAuthId))
      probe.send(apiActor, Received(codecRes2BS(req)))
      probe.expectMsg(Write(codecRes2BS(res)))
      success
    }

    "reply pong to ping" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      val pingVal = rand.nextLong()
      insertAuthId()
      Ping(pingVal) :~> @<~:(Pong(pingVal))
    }

    "send drop to package with invalid crc" in {
      implicit val (probe, apiActor) = probeAndActor()
      val req = hex"1e00000000000000010000000000000002000000000000000301f013bb3411"
      probe.send(apiActor, Received(req))
      val res = protoPackageBox.build(0L, 0L, 0L, Drop(0L, "invalid crc32"))
      probe.expectMsg(Write(res))
      success
    }

    "parse packages in single stream" in {
      val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      insertAuthAndSessionId()

      val messages = (1 to 100).map { _ => (Ping(rand.nextLong), rand.nextLong) }
      val packages = messages.map(t => pack(t._1, t._2))
      val req = packages.map(_.blob).foldLeft(ByteString.empty)(_ ++ _)
      req.grouped(7) foreach { buf =>
        probe.send(apiActor, Received(buf))
      }

      val expectedPongs = messages flatMap { t =>
        val pingVal = t._1.randomId
        val messageId = t._2
        val p = pack(Pong(pingVal), messageId)
        val ackPack = ack(messageId)
        Seq(p, ackPack)
      }
      probe.expectMsgAllOf(expectedPongs.map(p => Write(p.blob)) :_*)
      success
    }

//    "handle container with Ping's" in {
//      val (probe, apiActor) = probeAndActor()
//      val authId = rand.nextLong
//      val sessionId = rand.nextLong
//      val messages: Seq[MessageBox] = (1 to 100).map { _ =>
//        MessageBox(rand.nextLong, Ping(rand.nextLong))
//      }
//      val container = MessageBox(rand.nextLong, Container(messages))
//      val p = Package(authId, sessionId, container)
//      val packageBlob = Received(codecRes2BS(protoPackageBox.encode(p)))
//      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
//      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
//
//      probe.send(apiActor, packageBlob)
//
//      val expectedPongs = messages flatMap { message =>
//        val messageId = message.messageId
//        val pingVal = message.body.asInstanceOf[Ping].randomId
//        val p = Package(authId, sessionId, MessageBox(messageId, Pong(pingVal)))
//        val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
//        Seq(codecRes2BS(protoPackageBox.encode(p)), codecRes2BS(protoPackageBox.encode(ack)))
//      }
//      probe.expectMsgAllOf(expectedPongs.map(Write(_)) :_*)
//      success
//    }
//
//    "handle RPC request auth code" in {
//      val (probe, apiActor) = probeAndActor()
//      val authId = rand.nextLong()
//      val sessionId = rand.nextLong()
//      val messageId = rand.nextLong()
//      val rpcReq = RpcRequestBox(Request(RequestAuthCode(79853867016L, 123, "apikey")))
//      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
//      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
//      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
//      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
//
//      probe.send(apiActor, Received(ByteString(buf)))
//
//      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuthCode("12345", false)))
//      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
//      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
//      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
//      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
//      val expectMsgs = Seq(ackRes, res)
//      probe.expectMsgAllOf(expectMsgs :_*)
//      success
//    }
//
//    "handle RPC request sign up" in {
//      val (probe, apiActor) = probeAndActor()
//      val authId = rand.nextLong()
//      val sessionId = rand.nextLong()
//      val messageId = rand.nextLong()
//      val phone = 79853867016L
//
//      val rpcReq = RpcRequestBox(Request(RequestSignUp(phone, "wow", "such sms", "Timothy", Some("Klim"), hex"ac1d".bits)))
//      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
//      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
//      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
//      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
//      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, "wow", "such sms")).sync()
//
//      probe.send(apiActor, Received(ByteString(buf)))
//
//      val user = struct.User(1090901, 1, "Timothy", Some("Klim"), None, Seq(1L))
//      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
//      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
//      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
//      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
//      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
//      val expectMsgs = Seq(ackRes, res)
//      probe.expectMsgAllOf(expectMsgs :_*)
//      success
//    }
//
//    "handle RPC request sign in" in {
//      val (probe, apiActor) = probeAndActor()
//      val authId = rand.nextLong()
//      val sessionId = rand.nextLong()
//      val messageId = rand.nextLong()
//      val phone = 79853867016L
//      val pubKey = hex"ac1d".bits
//      val firstName = "Timothy"
//      val lastName = Some("Klim")
//      val userId = 1090901
//
//      val rpcReq = RpcRequestBox(Request(RequestSignIn(phone, "wow", "such sms", pubKey)))
//      val p = Package(authId, sessionId, MessageBox(messageId, rpcReq))
//      val buf = protoPackageBox.encode(p).toOption.get.toByteBuffer
//      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()
//      SessionIdRecord.insertEntity(SessionId(authId, sessionId)).sync()
//      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, "wow", "such sms")).sync()
//      UserRecord.insertEntity(Entity(userId, User.build(pubKey, firstName, lastName, NoSex)))
//      PhoneRecord.insertEntity(Phone(phone, userId))
//
//      probe.send(apiActor, Received(ByteString(buf)))
//
//      val user = struct.User(userId, 1L, "Timothy", Some("Klim"), None, Seq(1L))
//      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
//      val resP = Package(authId, sessionId, MessageBox(messageId, rpcRes))
//      val res = Write(ByteString(protoPackageBox.encode(resP).toOption.get.toByteBuffer))
//      val ack = Package(authId, sessionId, MessageBox(messageId, MessageAck(Array(messageId))))
//      val ackRes = Write(ByteString(protoPackageBox.encode(ack).toOption.get.toByteBuffer))
//      val expectMsgs = Seq(ackRes, res)
//      probe.expectMsgAllOf(expectMsgs :_*)
//      success
//    }
  }
}
