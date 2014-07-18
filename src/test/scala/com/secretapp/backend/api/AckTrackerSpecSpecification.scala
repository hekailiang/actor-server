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
import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps
import org.specs2.mutable.ActorSpecification

class AckTrackerSpecSpecification extends ActorSpecification {
  import system.dispatcher

  override lazy val actorSystemName = "api"

  val probe = TestProbe()
  val message = MessageBox(messageId = 123L, RequestAuthId())
  val bytes = ByteString(MessageBoxCodec.encode(message).toOption.get.toByteArray)

  def getTrackerActor() = system.actorOf(Props(new AckTrackerActor(10)))

  "AckTracker" should {
    "register new message" in {
      val tracker = getTrackerActor()
      probe.send(tracker, RegisterMessage(message.messageId, bytes))
      probe.send(tracker, GetUnackdMessages)
      val expected = immutable.Map[Long, ByteString]() + Tuple2(123L, bytes)
      probe.expectMsg(500.millis, UnackdMessages(expected))
      success
    }

    "track its status" in {
      val tracker = getTrackerActor()
      probe.send(tracker, RegisterMessageAck(123L))
      probe.send(tracker, GetUnackdMessages)
      val expected = immutable.Map[Long, ByteString]()
      probe.expectMsg(500.millis, UnackdMessages(expected))
      success
    }

    "forget when it's delivered" in {
      val tracker = getTrackerActor()
      probe.send(tracker, RegisterMessage(message.messageId, bytes))
      probe.send(tracker, RegisterMessage(124L, bytes))
      probe.expectMsg(500.millis, MessagesSizeOverflow)
      success
    }
  }
}
