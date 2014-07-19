package com.secretapp.backend.api

import akka.actor._
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
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import org.specs2.mutable.ActorSpecification

/* Write this spec when ActorSpecification will be compatible with CassandraSpecification
class UpdatesManagerSpec extends ActorSpecification with CassandraSpecification {
  import system.dispatcher

  override lazy val actorSystemName = "api"

  val probe = TestProbe()

  def getManager(keyHash: Long) = system.actorOf(Props(new UpdatesManager(keyHash)), s"$keyHash")

  "UpdatesManager" should {
    "increment its seq" in {
      val manager = getManager(20L)

      1 must equalTo(1)
    }
  }

}
 */
