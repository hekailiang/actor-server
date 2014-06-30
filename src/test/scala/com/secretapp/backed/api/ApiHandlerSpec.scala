package com.secretapp.backed.api

import com.secretapp.backend.protocol.codecs.{ResponseAuthId, RequestAuthId}
import org.scalatest._
import akka.testkit._
import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import com.secretapp.backend.api.ApiHandler
import com.secretapp.backend.protocol.Package
import java.util.concurrent.ConcurrentHashMap

class ApiHandlerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("api"))

  import system.dispatcher

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  val authTable = new ConcurrentHashMap[Long, Long]()
  val probe = TestProbe()
  val apiActor = system.actorOf(Props(classOf[ApiHandler], authTable))


  "actor" must {

    "reply pong to ping" in {
      val req = Package.encode(0L, 0L, 1L, RequestAuthId()).toOption.get.toByteBuffer
      val res = Package.encode(0L, 0L, 1L, ResponseAuthId(1L)).toOption.get.toByteBuffer
      probe.send(apiActor, Received(ByteString(req)))
      probe.expectMsg(Write(ByteString(res)))
    }

  }



}
