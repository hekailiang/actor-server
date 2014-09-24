package com.secretapp.backend.services.rpc.presence

import com.secretapp.backend.data.message.rpc.{Ok, ResponseVoid}
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.services.rpc.RpcSpec
import scala.collection.immutable

class PresenceServiceSpec extends RpcSpec {
  import system.dispatcher

  def assertResponseVoidReceived(implicit scope: TestScope) =
    receiveNWithAck(3)(scope.probe, scope.apiActor).exists { p =>
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

        SubscribeForOnline(immutable.Seq(UserId(scope2.user.uid, 0))) :~>! scope

        assertResponseVoidReceived
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

        SubscribeForOnline(immutable.Seq(UserId(2, 0))) :~>! scope

        protoReceiveN(2)(scope.probe, scope.apiActor)
      }

      {
        implicit val scope = scope2

        RequestSetOnline(true, 3000) :~> <~:[ResponseOnline]
        SubscribeForOnline(immutable.Seq(UserId(2, 0))) :~>! scope
        val received = protoReceiveN(1)(scope.probe, scope.apiActor)
      }

      {
        implicit val scope = scope1

        protoReceiveN(1)(scope.probe, scope.apiActor)
      }
    }
  }
}
