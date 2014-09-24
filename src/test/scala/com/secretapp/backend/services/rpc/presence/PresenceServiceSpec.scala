package com.secretapp.backend.services.rpc.updates

import akka.actor._
import akka.testkit._
import com.secretapp.backend.data.message.rpc.ResponseVoid
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
    "return ResponseVoid for subscribe" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1
        SubscribeForOnline(immutable.Seq(UserId(scope2.user.uid, 0))) :~> <~:[ResponseVoid]
      }
    }

    "return ResponseVoid for unsubscribe" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1
        UnsubscribeForOnline(immutable.Seq(UserId(scope2.user.uid, 0))) :~> <~:[ResponseVoid]
      }
    }

    "subscribe to updates and receive them" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1

        SubscribeForOnline(immutable.Seq(UserId(2, 0))) :~> <~:[ResponseVoid]

        protoReceiveN(2)(scope.probe, scope.apiActor)
      }

      {
        implicit val scope = scope2

        RequestSetOnline(true, 3000) :~> <~:[ResponseOnline]
        SubscribeForOnline(immutable.Seq(UserId(2, 0))) :~> <~:[ResponseVoid]

        val received = protoReceiveN(1)(scope.probe, scope.apiActor)
      }

      {
        implicit val scope = scope1

        protoReceiveN(1)(scope.probe, scope.apiActor)
      }
    }
  }
}
