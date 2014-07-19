package com.secretapp.backend.api

import akka.actor._
import akka.testkit._
import akka.persistence._
import akka.persistence.cassandra.journal._
import org.scalatest._

class UpdatesSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike
    with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("api"))

  import system.dispatcher

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
  val probe = TestProbe()
  //def getTrackerActor() = system.actorOf(Props(new AckTrackerActor(10)))
  val persistence = Persistence(system)
  val journal = system.actorOf(Props(new CassandraJournal))

  "Journal" must {
    "replay messages" in {

    }
  }


}
