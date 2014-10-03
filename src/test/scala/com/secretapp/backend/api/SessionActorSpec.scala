package com.secretapp.backend.api

import com.secretapp.backend.services.rpc.RpcSpec
import scala.language.{ postfixOps, higherKinds }
import scala.concurrent.duration._
import scala.collection.mutable
import com.newzly.util.testing.AsyncAssertionsHelper._
import akka.testkit._
import akka.actor.{ Props, Actor }
import akka.io.Tcp._
import akka.util.ByteString
import scodec.bits._
import scala.util.Random
import org.specs2.mutable.{ActorServiceHelpers, ActorLikeSpecification}
import org.specs2.matcher.TraversableMatchers
import org.scalamock.specs2.MockFactory
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.persist._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.types._
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.datastax.driver.core.{ Session => CSession }
import scalaz._
import Scalaz._

class SessionActorSpec extends RpcSpec {
  import system.dispatcher

  transportForeach { implicit transport =>
    "actor" should {
      "reply with auth token to auth request" in {
        implicit val (probe, apiActor) = getProbeAndActor()
        implicit val session = SessionIdentifier(0L)
        implicit val authId = 0L

        sendMsg(RequestAuthId())
        expectMsgByPF() {
          case ResponseAuthId(_) =>
        }
      }

      "reply pong to ping" in {
        implicit val (probe, apiActor) = getProbeAndActor()
        implicit val session = SessionIdentifier()
        implicit val authId = rand.nextLong()
        insertAuthId(authId)

        val pingVal = rand.nextLong()

        sendMsg(Ping(pingVal))
        expectMsg(Pong(pingVal), withNewSession = true)
      }

      "send drop when invalid package" in {
        implicit val (probe, apiActor) = getProbeAndActor()
        implicit val session = SessionIdentifier(0L)
        implicit val authId = 0L

        sendMsg(ByteString("_____________________________________!@#$%^"))
        expectMsgByPF() { case Drop(0L, _) => }
      }

      "handle container with Ping's" in {
        implicit val (probe, apiActor) = getProbeAndActor()
        implicit val session = SessionIdentifier()
        implicit val authId = rand.nextLong()
        insertAuthId(authId)

        var msgId = 0L
        val pingValQueue = mutable.Set[Long]()
        val messages = (1 to 100).map { _ =>
          val pingVal = rand.nextLong()
          pingValQueue += pingVal
          msgId += 4
          MessageBox(msgId, Ping(pingVal))
        }
        val container = MessageBox(0, Container(messages))

        sendMsgBox(container)
        expectMsgsWhileByPF(withNewSession = true) {
          case Pong(pingVal) =>
            pingValQueue -= pingVal
            pingValQueue.isEmpty
        }
      }
    }
  }
}
