package com.secretapp.backend.services.rpc.updates

import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.update
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.services.rpc.RpcSpec
import scodec.codecs.{ int32 => int32codec }
import scodec.bits._
import scala.collection.immutable
import scala.concurrent.duration._

class PresenceServiceSpec extends RpcSpec {
  import system.dispatcher

  "presence service" should {
    "subscribe to updates and receive them" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1

        SubscribeForOnline(immutable.Seq(UserId(2, 0))) :~>!(scope)

        val received = scope.probe.receiveN(2)
        println(s"RRECEIVED ${received}")
      }

      {
        implicit val scope = scope2

        RequestSetOnline(true, 3000) :~> <~:[ResponseOnline]
        SubscribeForOnline(immutable.Seq(UserId(2, 0))) :~>!(scope)

        val received = scope.probe.receiveN(1)
        println(s"RRECEIVED ${received}")
      }

      {
        implicit val scope = scope1

        scope.probe.receiveN(1)
      }
    }
  }
}
