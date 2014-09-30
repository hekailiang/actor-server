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

  def assertResponseVoidReceived(implicit scope: TestScope) =
    receiveNWithAck(2)(scope.probe, scope.apiActor).exists { p =>
      p.body match {
        case p: RpcResponseBox => p.body match {
          case p: Ok => p.body.isInstanceOf[ResponseVoid]
          case _ => false
        }
        case _ => false
      }
    } should beTrue

  "presence service" should {
    "return ResponseVoid for subscribe" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1

        SubscribeToOnline(immutable.Seq(UserId(scope2.user.uid, 0))) :~>! scope

        assertResponseVoidReceived
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
  }
}
