package com.secretapp.backend.api

import akka.actor._
import akka.testkit._
import akka.util.Timeout
import com.secretapp.backend.api.counters._
import scala.concurrent.duration._
import scala.language.postfixOps
import org.specs2.mutable.ActorSpecification

class CounterActorSpec extends ActorSpecification with ImplicitSender {
  import CounterProtocol._

  def getCounterActor(name: String) = system.actorOf(Props(new CounterActor(name)), name)

  implicit val timeout = Timeout(15.seconds)

  "CounterActor" should {
    "get state" in {
      val counter = getCounterActor(s"incrementer-${System.nanoTime()}")
      counter ! Get
      expectMsg(timeout.duration, 0)
    }

    "increment its state" in {
      val counter = getCounterActor(s"incrementer-${System.nanoTime()}")

      counter ! GetNext
      expectMsg(timeout.duration, 1)
    }

    "get bulk" in {
      val counter = getCounterActor(s"incrementer-${System.nanoTime()}")

      counter ! GetNext
      counter ! GetBulk(100)
      expectMsg(timeout.duration, 1)

      expectMsg(timeout.duration, Bulk(2, 101))
    }

    "recover its state" in {
      val name = s"incrementer-failover-${System.nanoTime()}"
      val counter = getCounterActor(name)

      counter ! GetNext
      counter ! GetNext
      counter ! GetNext
      counter ! GetBulk(100)
      counter ! GetNext

      expectMsgAllOf(timeout.duration, 1, 2, 3, Bulk(4, 103), 104)

      system.stop(counter)

      Thread.sleep(100)

      val recoveredCounter = getCounterActor(name)

      recoveredCounter ! GetNext

      expectMsg(timeout.duration, 105)
    }
  }
}
