package com.secretapp.backend.api

import com.secretapp.backend.data.message.struct.PeerType
import com.secretapp.backend.util.ACL
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.services.rpc.RpcSpec
import com.websudos.util.testing._
import scala.collection.immutable
import scala.language.higherKinds
import scodec.bits._

class GroupMessagingSpec extends RpcSpec {
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

  "GroupMessaging" should {
    "send invites on group creation" in {
      val (scope1, scope2) = TestScope.pair()

      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)

      {
        implicit val scope = scope1

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(1)
        val upd = diff.updates.last.body.assertInstanceOf[GroupInvite]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.inviterUserId should_==(scope1.user.uid)
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(1)
        val upd = diff.updates.last.body.assertInstanceOf[GroupInvite]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.inviterUserId should_==(scope1.user.uid)
      }
    }

    "send invites on group invitation" in {
      val (scope1, scope2) = TestScope.pair()

      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector())(scope1)

      {
        implicit val scope = scope1

        RequestInviteUser(
          groupOutPeer = struct.GroupOutPeer(respGroup.groupPeer.id, respGroup.groupPeer.accessHash),
          randomId = rand.nextLong,
          user = struct.UserOutPeer(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user))
        ) :~> <~:[ResponseSeqDate]

        Thread.sleep(1000)

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[GroupUserAdded]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.inviterUserId should_==(scope1.user.uid)
        upd.userId should_==(scope2.user.uid)
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(4)

        {
          val upd = diff.updates.head.body.assertInstanceOf[GroupInvite]
          upd.groupId should_==(respGroup.groupPeer.id)
          upd.inviterUserId should_==(scope1.user.uid)
        }

        diff.updates(1).body.assertInstanceOf[GroupTitleChanged]
        diff.updates(2).body.assertInstanceOf[GroupAvatarChanged]

        {
          val upd = diff.updates(3).body.assertInstanceOf[GroupMembersUpdate]
          upd.groupId should_==(respGroup.groupPeer.id)
          upd.members.toSet should_==(Set(scope1.user.uid, scope2.user.uid))
        }
      }
    }

    "not allow to invite user twice" in {
      val (scope1, scope2) = TestScope.pair()

      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)

      {
        implicit val scope = scope1

        RequestInviteUser(
          groupOutPeer = struct.GroupOutPeer(respGroup.groupPeer.id, respGroup.groupPeer.accessHash),
          randomId = rand.nextLong,
          user = struct.UserOutPeer(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user))
        ) :~> <~:(400, "USER_ALREADY_INVITED")
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
        // check if there are doubled invitation updates
        diff.updates.length should beEqualTo(1)
      }
    }

    "send updates on group leave" in {
      val (scope1, scope2) = TestScope.pair()

      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)

      {
        implicit val scope = scope2

        RequestLeaveGroup(
          struct.GroupOutPeer(respGroup.groupPeer.id, respGroup.groupPeer.accessHash),
          rand.nextLong
        ) :~> <~:[ResponseSeqDate]


        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[GroupUserLeave]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.userId should_==(scope2.user.uid)
      }

      {
        implicit val scope = scope1

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[GroupUserLeave]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.userId should_==(scope2.user.uid)
      }
    }

    "send updates on group kick" in {
      val (scope1, scope2) = TestScope.pair()

      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)

      {
        implicit val scope = scope1

        RequestKickUser(
          struct.GroupOutPeer(respGroup.groupPeer.id, respGroup.groupPeer.accessHash),
          rand.nextLong,
          struct.UserOutPeer(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user))
        ) :~> <~:[ResponseSeqDate]


        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[GroupUserKick]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.userId should_==(scope2.user.uid)
        upd.kickerUid should_==(scope.user.uid)
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[GroupUserKick]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.userId should_==(scope2.user.uid)
        upd.kickerUid should_==(scope1.user.uid)
      }
    }

    "deliver messages into group" in {
      val (scope1, scope2) = TestScope.pair()

      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)

      {
        implicit val scope = scope1

        RequestSendMessage(
          outPeer = struct.OutPeer.group(respGroup.groupPeer.id, respGroup.groupPeer.accessHash),
          randomId = 1L,
          message = TextMessage("Yolo!")
        ) :~> <~:[ResponseSeqDate]

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[MessageSent]
        upd.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[Message]
        upd.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
        upd.senderUid should_==(scope1.user.uid)
        upd.randomId should_==(1L)
        upd.message should_==(TextMessage("Yolo!"))
      }
    }

    "send updates on title change" in {
      val (scope1, scope2) = TestScope.pair()
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)

      {
        implicit val scope = scope1

        RequestEditGroupTitle(
          groupOutPeer = struct.GroupOutPeer(respGroup.groupPeer.id, respGroup.groupPeer.accessHash),
          randomId = rand.nextLong,
          title = "Group 4000"
        ) :~> <~:[ResponseSeqDate]

        val (diff, _) = RequestGetDifference(respGroup.seq, respGroup.state) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(1)
        val upd = diff.updates.last.body.assertInstanceOf[GroupTitleChanged]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.title should_==("Group 4000")
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[GroupTitleChanged]
        upd.groupId should_==(respGroup.groupPeer.id)
        upd.title should_==("Group 4000")
      }
    }

    "send updates on name change" in {
      val (scope1, scope2) = TestScope.pair()
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1
        val respGroup = createGroup(Vector(scope2.user))(scope1)

        Thread.sleep(500)

        RequestEditGroupTitle(
          struct.GroupOutPeer(
            respGroup.groupPeer.id,
            respGroup.groupPeer.accessHash
          ),
          randomId = rand.nextLong,
          title = "Title 3000"
        ) :~> <~:[ResponseSeqDate]

        Thread.sleep(500)

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        diff.updates.last.body.assertInstanceOf[GroupTitleChanged]
      }

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        diff.updates.last.body.assertInstanceOf[GroupTitleChanged]
      }
    }
  }
}
