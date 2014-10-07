package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.testkit._
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.message.rpc.{ Ok, ResponseVoid }
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.update
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update.{ UserLastSeen, UserOffline, UserOnline }
import com.secretapp.backend.data.message.update.WeakUpdate
import com.secretapp.backend.data.models.User
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.rpc.RpcSpec
import scala.collection.immutable
import scala.concurrent.duration._

class PresenceServiceSpec extends RpcSpec {
  import system.dispatcher

  "presence service" should {
    /*
    "return ResponseVoid for subscribe" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1

        SubscribeToOnline(immutable.Seq(UserId(scope2.user.uid, 0))) :~> <~:[ResponseVoid]
      }
    }

    "return ResponseVoid for unsubscribe" in {
      val (scope1, scope2) = TestScope.pair(3, 4)

      {
        implicit val scope = scope1
        UnsubscribeFromOnline(immutable.Seq(UserId(scope2.user.uid, 0))) :~> <~:[ResponseVoid]
      }
    }

    "subscribe to updates and receive them" in {
      val (scope1, scope2) = TestScope.pair(5, 6)
      val duration = DurationInt(1).seconds

      {
        implicit val scope = scope1

        SubscribeToOnline(immutable.Seq(UserId(6, 0))) :~> <~:[ResponseVoid]

        scope.probe.expectNoMsg(duration)
      }

      {
        implicit val scope = scope2

        RequestSetOnline(true, 3000) :~> <~:[ResponseVoid]
        SubscribeToOnline(immutable.Seq(UserId(5, 0))) :~> <~:[ResponseVoid]

        scope.probe.expectNoMsg(duration)
      }

      {
        implicit val scope = scope1

        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        val updBox = MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.asInstanceOf[UpdateBox]
        val update = updBox.body.asInstanceOf[WeakUpdate]
        val offlineUpdate = update.body.asInstanceOf[UserOnline]
        offlineUpdate.uid should equalTo(6)
        scope.probe.expectNoMsg(duration)
      }
    }
*/
    "tell presences on subscription" in {
      val (scope1, scope2) = TestScope.pair(7, 8)
      val duration = DurationInt(1).seconds

      {
        implicit val scope = scope1

        SubscribeToOnline(immutable.Seq(UserId(8, 0))) :~> <~:[ResponseVoid]

        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[UserOffline]

        RequestSetOnline(true, 3000) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        SubscribeToOnline(immutable.Seq(UserId(7, 0))) :~> <~:[ResponseVoid]
        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)

        mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[UserOnline]
      }
    }
  }
}
