package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import akka.util.{ ByteString, Timeout }
import akka.pattern.ask
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message._
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import org.specs2.mutable.ActorSpecification

class CounterActorSpec extends ActorSpecification with ImplicitSender {
  import system.dispatcher
  import CounterProtocol._

  override lazy val actorSystemName = "api"

  val probe = TestProbe()

  def getCounterActor(name: String) = system.actorOf(Props(new CounterActor(name)), name)

  implicit val timeout = Timeout(5.seconds)

  "CounterActor" should {
    "increment its state" in {
      val counter = getCounterActor(s"incrementer-${System.nanoTime()}")

      probe.send(counter, GetNext)
      probe.expectMsg(5.seconds, 1L)
      success
    }

    "recover its state" in {
      val name = s"incrementer-failover-${System.nanoTime()}"
      val counter = getCounterActor(name)
      println(counter.path.toStringWithoutAddress)

      val selection = system.actorSelection(s"/user/${name}")

      selection ! GetNext
      selection ! GetNext
      selection ! GetNext

      expectMsgAllOf(5.seconds, 1L, 2L, 3L)

      system.stop(counter)

      Thread.sleep(1000)
      val recoveredCounter = getCounterActor(name)

      selection ! GetNext

      expectMsg(5.seconds, 4L)

      success
    }
  }
}
