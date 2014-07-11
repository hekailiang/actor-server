package com.secretapp.backend.api

import com.secretapp.backend.protocol.codecs._
import org.scalatest._
import akka.testkit._
import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.persist.CassandraWordSpec

class ApiHandlerSpec extends TestKit(ActorSystem("api")) with ImplicitSender with CassandraWordSpec
{

  import system.dispatcher

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val probe = TestProbe()
  def getApiActor() = system.actorOf(Props(new ApiHandler(probe.ref) {
    override lazy val rand = new Random() {
      override def nextLong() = 12345L
    }
  }))


  "actor" must {

    "reply with auth token to auth request" in {
      val apiActor = getApiActor()
      val req = protoPackageBox.build(0L, 0L, 1L, RequestAuthId())
      val res = protoPackageBox.build(0L, 0L, 1L, ResponseAuthId(12345L))
      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))
    }

    "reply pong to ping" in {
//      TODO: DRY
      val apiActor = getApiActor()
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
      val apiActor = getApiActor()
      val req = hex"1e00000000000000010000000000000002000000000000000301f013bb3411".bits.toByteBuffer
      val res = protoPackageBox.build(0L, 0L, 0L, Drop(0L, "invalid crc32")).toOption.get.toByteBuffer
      probe.send(apiActor, Received(ByteString(req)))
      probe.expectMsg(Write(ByteString(res)))
    }

  }



}
