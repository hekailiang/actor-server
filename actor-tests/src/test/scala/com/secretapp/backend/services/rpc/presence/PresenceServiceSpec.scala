package com.secretapp.backend.services.rpc.presence

import akka.actor._
import akka.testkit._
import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.message.rpc.{ Ok, ResponseVoid }
import com.secretapp.backend.data.message.rpc.presence._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.update
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.duration._
import scodec.bits._

class PresenceServiceSpec extends RpcSpec {
  import system.dispatcher

  "presence service" should {
    "return ResponseVoid for subscribe" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestSubscribeToOnline(immutable.Seq(struct.UserOutPeer(scope2.user.uid, 0))) :~> <~:[ResponseVoid]
      }
    }

    "return ResponseVoid for unsubscribe" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1
        RequestSubscribeFromOnline(immutable.Seq(struct.UserOutPeer(scope2.user.uid, 0))) :~> <~:[ResponseVoid]
      }
    }

    "subscribe to updates and receive them" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(5, 6)
      catchNewSession(scope1)
      catchNewSession(scope2)
      val duration = DurationInt(1).seconds

      {
        implicit val scope = scope1

        RequestSubscribeToOnline(immutable.Seq(struct.UserOutPeer(6, 0))) :~> <~:[ResponseVoid]

        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[UserOffline]
      }

      {
        implicit val scope = scope2

        RequestSetOnline(true, 3000) :~> <~:[ResponseVoid]
        RequestSubscribeToOnline(immutable.Seq(struct.UserOutPeer(5, 0))) :~> <~:[ResponseVoid]

        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[UserOffline]
      }

      {
        implicit val scope = scope1

        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        val updBox = MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.asInstanceOf[UpdateBox]
        val update = updBox.body.asInstanceOf[WeakUpdate]
        val offlineUpdate = update.body.asInstanceOf[UserOnline]
        offlineUpdate.userId should equalTo(6)
        scope.probe.expectNoMsg(duration)
      }
    }

    "tell presences on subscription" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(7, 8)
      catchNewSession(scope1)
      catchNewSession(scope2)
      val duration = DurationInt(1).seconds

      {
        implicit val scope = scope1

        RequestSubscribeToOnline(immutable.Seq(struct.UserOutPeer(8, 0))) :~> <~:[ResponseVoid]

        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[UserOffline]

        RequestSetOnline(true, 3000) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        RequestSubscribeToOnline(immutable.Seq(struct.UserOutPeer(7, 0))) :~> <~:[ResponseVoid]

        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[UserOnline]
      }
    }

    "count group presences" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      val groupId = createGroup(List(scope.user)).groupPeer.id

      Thread.sleep(1000)

      val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.ResponseGetDifference]

      RequestSetOnline(true, 3000) :~> <~:[ResponseVoid]
      RequestSubscribeToGroupOnline(immutable.Seq(struct.GroupOutPeer(groupId, 0))) :~> <~:[ResponseVoid]

      {
        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        val go = mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[GroupOnline]
        go.count should beEqualTo(1)
      }

      Thread.sleep(3100)

      {
        val (mb :: _) = receiveNMessageBoxes(1)(scope.probe, scope.apiActor)
        val go = mb.body.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[GroupOnline]
        go.count should beEqualTo(0)
      }
    }
  }

  def createGroup(users: immutable.Seq[models.User])(implicit scope: TestScope): ResponseCreateGroup = {
    val rqCreateGroup = RequestCreateGroup(
      randomId = 1L,
      title = "Group 3000",
      users = users map { user =>
        struct.UserOutPeer(user.uid, ACL.userAccessHash(scope.user.authId, user))
      }
    )

    val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

    resp
  }
}
