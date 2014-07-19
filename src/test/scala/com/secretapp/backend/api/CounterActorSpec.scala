package com.secretapp.backend.api

import akka.actor._
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

class CounterActorSpec extends ActorSpecification {
  import system.dispatcher
  import CounterProtocol._

  override lazy val actorSystemName = "api"

  val probe = TestProbe()

  def getCounterActor(name: String) = system.actorOf(Props(new CounterActor(name)))

  "CounterActor" should {
    "increment its state" in {
      val counter = getCounterActor(s"incrementer-${System.nanoTime()}")

      probe.send(counter, GetNext)
      probe.expectMsg(5.seconds, CounterState(1L))
      success
    }

    "recover its state" in {
      val name = s"incrementer-failover-${System.nanoTime()}"
      val counter = getCounterActor(name)

      probe.send(counter, GetNext)
      probe.send(counter, GetNext)
      probe.send(counter, GetNext)
      probe.expectMsgAllOf(5.seconds, CounterState(1L), CounterState(2L), CounterState(3L))

      system.stop(counter)

      val recoveredCounter = getCounterActor(name)

      probe.send(recoveredCounter, GetNext)
      probe.expectMsg(5.seconds, CounterState(4L))

      success
    }
  }
}
