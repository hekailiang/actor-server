package com.secretapp.backed.api

import com.secretapp.backend.protocol.codecs.{ResponseAuthId, RequestAuthId}
import org.scalatest._
import akka.testkit._
import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import com.secretapp.backend.api.ApiHandler
import com.secretapp.backend.protocol.Package
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
  val apiActor = system.actorOf(Props(new ApiHandler(authTable) {
    override lazy val rand = new Random() {
      override def nextLong() = 12345L
    }
  }))


  "actor" must {

    "reply pong to ping" in {
      val req = Package.encode(0L, 0L, 1L, RequestAuthId()).toOption.get.toByteBuffer
      val res = Package.encode(0L, 0L, 1L, ResponseAuthId(12345L)).toOption.get.toByteBuffer
      probe.send(apiActor, Received(ByteString(req)))
      probe.expectMsg(Write(ByteString(res)))
    }

  }



}
