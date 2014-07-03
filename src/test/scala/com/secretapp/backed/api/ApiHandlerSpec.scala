package com.secretapp.backed.api

import com.secretapp.backend.protocol.codecs._
import org.scalatest._
import akka.testkit._
import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import com.secretapp.backend.api.ApiHandler
import com.secretapp.backend.data._
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentSkipListSet }

class ApiHandlerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("api"))

  import system.dispatcher

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val authTable = new ConcurrentHashMap[Long, ConcurrentSkipListSet[Long]]()
  val probe = TestProbe()
  def getApiActor() = system.actorOf(Props(new ApiHandler(authTable) {
    override lazy val rand = new Random() {
      override def nextLong() = 12345L
    }
  }))


  "actor" must {

    "reply with auth token to auth request" in {
      val apiActor = getApiActor()
      val req = PackageCodec.encode(0L, 0L, 1L, RequestAuthId())
      val res = PackageCodec.encode(0L, 0L, 1L, ResponseAuthId(12345L))
      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))
    }

    "reply pong to ping" in {
//      TODO: DRY
      val apiActor = getApiActor()
      val authId = 12345L
      val messageId = 1L
      val req = PackageCodec.encode(0L, 0L, messageId, RequestAuthId())
      val res = PackageCodec.encode(0L, 0L, messageId, ResponseAuthId(authId))
      probe.send(apiActor, Received(ByteString(req.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(res.toOption.get.toByteBuffer)))

      val randId = 987654321L
      val sessionId = 123L
      val ping = PackageCodec.encode(authId, sessionId, messageId + 1, Ping(randId))
      val newNewSession = PackageCodec.encode(authId, sessionId, messageId + 1, NewSession(sessionId, messageId + 1))
      val pong = PackageCodec.encode(authId, sessionId, messageId + 1, Pong(randId))
      val reply = newNewSession.toOption.get ++ pong.toOption.get

      probe.send(apiActor, Received(ByteString(ping.toOption.get.toByteBuffer)))
      probe.expectMsg(Write(ByteString(reply.toByteBuffer)))
    }

  }



}
