package com.secretapp.backend.api

import com.secretapp.backend.data.models.AuthId
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.protocol.codecs._
import org.scalatest._
import akka.testkit._
import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import scala.collection.mutable
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.persist.{AuthIdRecord, CassandraWordSpec}
import com.newzly.util.testing.AsyncAssertionsHelper._

class ApiHandlerSpec extends TestKit(ActorSystem("api")) with ImplicitSender with CassandraWordSpec
{

  import system.dispatcher

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  def probeAndActor() = {
    val probe = TestProbe()
    val actor = system.actorOf(Props(new ApiHandler(probe.ref, session) {
      override lazy val rand = new Random() {
        override def nextLong() = 12345L
      }
    }))
    (probe, actor)
  }


  "actor" must {

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

//    "reply pong to ping (10 times)" in {
//      //      TODO: DRY
//      val (probe, apiActor) = probeAndActor()
//      val authId = 12345L
//      val messageId = 1L
//      val req = protoPackageBox.build(0L, 0L, messageId, RequestAuthId())
//      val res = protoPackageBox.build(0L, 0L, messageId, ResponseAuthId(authId))
//      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
//      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))
//
//      val randId = 987654321L
//      val sessionId = 123L
//      for (i <- 1 to 10) {
//        val ping = protoPackageBox.build(authId, sessionId, messageId + 1, Ping(randId))
//        probe.send(apiActor, Received(ByteString(ping.toOption.get.toByteBuffer)))
//      }
//      val newNewSession = protoPackageBox.build(authId, sessionId, messageId + 1, NewSession(sessionId, messageId + 1))
//      probe.expectMsg(Write(ByteString(newNewSession.toOption.get.toByteBuffer)))
//
//      for (i <- 1 to 10) {
//        val pong = protoPackageBox.build(authId, sessionId, messageId + 1, Pong(randId))
//        probe.expectMsg(Write(ByteString(pong.toOption.get.toByteBuffer)))
//      }
//    }

    "send drop to invalid crc" in {
      val (probe, apiActor) = probeAndActor()
      val req = hex"1e00000000000000010000000000000002000000000000000301f013bb3411".bits.toByteBuffer
      val res = protoPackageBox.build(0L, 0L, 0L, Drop(0L, "invalid crc32")).toOption.get.toByteBuffer
      probe.send(apiActor, Received(ByteString(req)))
      probe.expectMsg(Write(ByteString(res)))
    }


    "parse packages in single stream" in { // TODO: replace by scalacheck
      val (probe, apiActor) = probeAndActor()

      val ids = new mutable.Queue[(Long, Long)]
      ids += ((150, 213674937025390L), (151, 213674988049640L), (152, 213674988971057L), (153, 213674991707182L))

      val authId = 5035258019635449406L
      val sessionId = -2911383367460896512L
      AuthIdRecord.insertEntity(AuthId(authId, None)).sync()

      val req = List(
        Package(authId,sessionId,MessageBox(150,Ping(213674937025390L))),
        Package(authId,sessionId,MessageBox(151,Ping(213674988049640L))),
        Package(authId,sessionId,MessageBox(152,Ping(213674988971057L))),
        Package(authId,sessionId,MessageBox(153,Ping(213674991707182L)))
      ).map { p => protoPackageBox.encode(p).toOption.get }.foldLeft(BitVector.empty)(_ ++ _)

      probe.send(apiActor, Received(ByteString(req.toByteBuffer)))
      val newNewSession = protoPackageBox.build(authId, sessionId, 150, NewSession(sessionId, 150))
      probe.expectMsg(Write(ByteString(newNewSession.toOption.get.toByteBuffer)))


      val res = List(
        Package(authId,sessionId,MessageBox(150,Pong(213674937025390L))),
        Package(authId,sessionId,MessageBox(151,Pong(213674988049640L))),
        Package(authId,sessionId,MessageBox(152,Pong(213674988971057L))),
        Package(authId,sessionId,MessageBox(153,Pong(213674991707182L)))
      ).map { p => ByteString(protoPackageBox.encode(p).toOption.get.toByteBuffer) }
      for (r <- res) {
        probe.expectMsg(Write(r))
      }
    }
  }
}
