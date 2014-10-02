package com.secretapp.backend.api

import akka.actor.{ ActorSystem, Props }
import akka.io.Tcp._
import akka.testkit._
import akka.util.Timeout
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message._
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.session.{ AckTrackerActor, AckTrackerProtocol }
import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps
import org.specs2.mutable.ActorSpecification
import scodec.bits.BitVector

class AckTrackerActorSpec extends ActorSpecification with RandomService {
  import system.dispatcher
  import AckTrackerProtocol._

  override lazy val actorSystemName = "api"

  val probe = TestProbe()
  val message = MessageBox(messageId = 123L, RequestAuthId())
  val bytes = MessageBoxCodec.encode(message).toOption.get

  def getTrackerActor() = system.actorOf(Props(new AckTrackerActor(rand.nextLong, rand.nextLong, 19)))

  "AckTracker" should {
    "register new message" in {
      val tracker = getTrackerActor()
      probe.send(tracker, RegisterMessage(message.messageId, bytes))
      probe.send(tracker, GetUnackdMessages)
      val expected = immutable.Map[Long, BitVector]() + Tuple2(123L, bytes)
      probe.expectMsg(10.seconds, UnackdMessages(expected))
    }

    "track its status" in {
      val tracker = getTrackerActor()
      probe.send(tracker, RegisterMessageAck(123L))
      probe.send(tracker, GetUnackdMessages)
      val expected = immutable.Map[Long, BitVector]()
      probe.expectMsg(5.seconds, UnackdMessages(expected))
    }

    "forget when it's delivered" in {
      val tracker = getTrackerActor()
      probe.send(tracker, RegisterMessage(message.messageId, bytes))
      probe.send(tracker, RegisterMessage(124L, bytes))
      probe.expectMsg(5.seconds, MessagesSizeOverflow)
    }
  }
}
