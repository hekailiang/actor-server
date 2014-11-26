package com.secretapp.backend.api

import akka.actor._
import akka.contrib.pattern.ClusterSingletonProxy
import akka.io.Tcp._
import akka.testkit._
import akka.util.{ ByteString, Timeout }
import akka.pattern.ask
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.persist.CassandraSpecification
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.protocol.codecs.message._
import com.secretapp.backend.api.counters._
import org.specs2.mutable.ActorLikeSpecification
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import org.specs2.mutable.ActorSpecification

class SocialBrokerSpec extends ActorLikeSpecification with CassandraSpecification with ImplicitSender {
  import system.dispatcher

  import SocialProtocol._

  implicit val timeout = Timeout(Duration(20, "seconds"))

  "SocialBroker" should {
    "count relations" in {
      val uid1 = 1
      val uid2 = 2

      val region = SocialBroker.startRegion()

      Await.result(region ? SocialMessageBox(uid1, GetRelations), timeout.duration) must equalTo(Set.empty)

      region ! SocialMessageBox(uid1, RelationsNoted((1 to 10).toSet))

      region ! SocialMessageBox(uid2, RelationsNoted((42 to 45).toSet))

      region ! SocialMessageBox(uid1, RelationsNoted((9 to 15).toSet))

      region ! SocialMessageBox(uid2, RelationsNoted((40 to 43).toSet))

      Await.result(region ? SocialMessageBox(uid2, GetRelations), timeout.duration) must equalTo((40 to 45).toSet)
      Await.result(region ? SocialMessageBox(uid1, GetRelations), timeout.duration) must equalTo((2 to 15).toSet)
    }
  }
}
