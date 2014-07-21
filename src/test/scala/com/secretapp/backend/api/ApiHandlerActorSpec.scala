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
import org.specs2.matcher.TraversableMatchers
import org.scalamock.specs2.MockFactory
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
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
import com.datastax.driver.core.{ Session => CSession }
import scalaz._
import Scalaz._

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

  val smsCode = "testsmscode"
  val smsHash = "testsmshash"
  val userId = 101

  trait GeneratorServiceMock extends GeneratorService {
    override def genNewAuthId = mockAuthId
    override def genSmsCode = smsCode
    override def genSmsHash = smsHash
    override def genUserId = userId
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
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      insertAuthAndSessionId()
      val messages = (1 to 100).map { _ => MessageBox(rand.nextLong, Ping(rand.nextLong)) }
      val packages = messages.map(pack(_))
      val req = packages.map(_.blob).foldLeft(ByteString.empty)(_ ++ _)
      req.grouped(7) foreach { buf =>
        probe.send(apiActor, Received(buf))
      }
      val expectedPongs = messages map { m =>
        val messageId = m.messageId
        val pingVal = m.body.asInstanceOf[Ping].randomId
        MessageBox(messageId, Pong(pingVal))
      }
      expectMsgWithAck(expectedPongs :_*)
    }

    "handle container with Ping's" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      insertAuthAndSessionId()

      val messages = (1 to 100).map { _ => MessageBox(rand.nextLong, Ping(rand.nextLong)) }
      val container = MessageBox(rand.nextLong, Container(messages))
      val packageBlob = pack(container)
      send(packageBlob)

      val expectedPongs = messages map { m =>
        val messageId = m.messageId
        val pingVal = m.body.asInstanceOf[Ping].randomId
        MessageBox(messageId, Pong(pingVal))
      }
      expectMsgWithAck(expectedPongs :_*)
    }

    "handle RPC request auth code" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val session = SessionIdentifier()
      insertAuthAndSessionId()

      val rpcReq = RpcRequestBox(Request(RequestAuthCode(79853867016L, rand.nextInt, rand.nextString(10))))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuthCode("testsmshash", false)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "handle RPC request sign up" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val phone = 79853867016L
      insertAuthAndSessionId()
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, smsHash, smsCode)).sync()

      val rpcReq = RpcRequestBox(Request(RequestSignUp(phone, smsHash, smsCode, "Timothy", Some("Klim"), hex"ac1d".bits)))
      val messageId = rand.nextLong
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val user = struct.User(userId, 1, "Timothy", Some("Klim"), None, Seq(1L))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }

    "handle RPC request sign in" in {
      implicit val (probe, apiActor) = probeAndActor()
      implicit val sessionId = SessionIdentifier()
      val messageId = rand.nextLong()
      val phone = 79853867016L
      val pubKey = hex"ac1d".bits
      val firstName = "Timothy"
      val lastName = Some("Klim")
      insertAuthAndSessionId(userId.some)
      AuthSmsCodeRecord.insertEntity(AuthSmsCode(phone, smsHash, smsCode)).sync()
      UserRecord.insertEntity(User.build(userId, pubKey, firstName, lastName, NoSex))
      PhoneRecord.insertEntity(Phone(phone, userId))

      val rpcReq = RpcRequestBox(Request(RequestSignIn(phone, smsHash, smsCode, pubKey)))
      val packageBlob = pack(MessageBox(messageId, rpcReq))
      send(packageBlob)

      val user = struct.User(userId, 1L, firstName, lastName, None, Seq(1L))
      val rpcRes = RpcResponseBox(messageId, Ok(ResponseAuth(123, user)))
      val expectMsg = MessageBox(messageId, rpcRes)
      expectMsgWithAck(expectMsg)
    }
  }
}
