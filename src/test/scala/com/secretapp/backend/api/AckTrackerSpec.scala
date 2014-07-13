package com.secretapp.backend.api

import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.testkit._
import akka.util.ByteString
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message._
import org.scalatest._
import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps

class AckTrackerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("api"))

  import system.dispatcher

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
  val probe = TestProbe()
  def getTrackerActor() = system.actorOf(Props(new AckTrackerActor(10)))

  "AckTracker" must {
    "register new message, track its status and forget when it's delivered" in {
      val tracker = getTrackerActor()
      val message = MessageBox(messageId = 123L, RequestAuthId())
      val bytes = ByteString(MessageBoxCodec.encode(message).toOption.get.toByteArray)
      val size = bytes.length

      // TODO: split to various "musts"
      {
        probe.send(tracker, RegisterMessage(message.messageId, bytes))
        probe.send(tracker, GetUnackdMessages)
        val expected = immutable.Map[Long, ByteString]() + Tuple2(123L, bytes)
        probe.expectMsg(500 millis, UnackdMessages(expected))
      }

      {
        probe.send(tracker, RegisterMessageAck(123L))
        probe.send(tracker, GetUnackdMessages)
        val expected = immutable.Map[Long, ByteString]()
        probe.expectMsg(500 millis, UnackdMessages(expected))
      }

      {
        probe.send(tracker, RegisterMessage(message.messageId, bytes))
        probe.send(tracker, RegisterMessage(124L, bytes))
        probe.expectMsg(500 millis, MessagesSizeOverflow)
      }
    }
  }
}
