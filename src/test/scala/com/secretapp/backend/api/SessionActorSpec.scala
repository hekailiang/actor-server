package com.secretapp.backend.api

import akka.actor.{ Props, Actor }
import akka.io.Tcp._
import akka.testkit._
import akka.util.ByteString
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.rpc.RpcSpec
import com.websudos.util.testing._
import org.specs2.matcher.TraversableMatchers
import org.specs2.mutable.{ActorServiceHelpers, ActorLikeSpecification}
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.{ postfixOps, higherKinds }
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._

class SessionActorSpec extends RpcSpec {
  import system.dispatcher

  transportForeach { implicit transport =>
    "actor" should {
      "reply with auth token to auth request" in {
        val (probe, apiActor) = getProbeAndActor()
        implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = SessionIdentifier(0L), authId = 0L)

        sendMsg(RequestAuthId())
        expectMsgByPF() {
          case ResponseAuthId(_) =>
        }
      }

      "reply pong to ping" in {
        implicit val scope = genTestScope()
        insertAuthId(scope.authId)
        val pingVal = rand.nextLong()

        sendMsg(Ping(pingVal))
        expectMsg(Pong(pingVal), withNewSession = true)
      }

      "send drop when invalid package" in {
        val (probe, apiActor) = getProbeAndActor()
        implicit val scope = TestScopeNew(probe = probe, apiActor = apiActor, session = SessionIdentifier(0L), authId = 0L)

        sendMsg(ByteString("_____________________________________!@#$%^"))
        expectMsgByPF() { case Drop(0L, _) => }
      }

      "handle container with Ping's" in {
        implicit val scope = genTestScope()
        insertAuthId(scope.authId)

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
            !pingValQueue.isEmpty
        }
        assert(pingValQueue.isEmpty)
      }
    }
  }
}
