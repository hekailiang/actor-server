package com.secretapp.backend.api

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.collection.immutable

trait GroupSpecHelpers {
  this: RpcSpec =>

  protected def createGroup(users: immutable.Seq[models.User])(implicit scope: TestScope): ResponseCreateGroup = {
    val rqCreateGroup = RequestCreateGroup(
      randomId = 1L,
      title = "Group 3000",
      users = users map { user =>
        struct.UserOutPeer(user.uid, ACL.userAccessHash(scope.user.authId, user))
      }
    )

    val (r, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

    r
  }

  protected def kickUser(groupPeer: struct.GroupOutPeer, user: models.User)(implicit scope: TestScope): ResponseSeqDate = {
    val (r, _) = RequestKickUser(
      groupPeer,
      rand.nextLong,
      struct.UserOutPeer(user.uid, ACL.userAccessHash(scope.user.authId, user))
    ) :~> <~:[ResponseSeqDate]

    r
  }

  protected def inviteUser(groupPeer: struct.GroupOutPeer, user: models.User)(implicit scope: TestScope): ResponseSeqDate = {
    val (r, _) = RequestInviteUser(
      groupOutPeer = struct.GroupOutPeer(groupPeer.id, groupPeer.accessHash),
      randomId = rand.nextLong,
      user = struct.UserOutPeer(user.uid, ACL.userAccessHash(scope.user.authId, user))
    ) :~> <~:[ResponseSeqDate]
    r
  }

  protected def leaveGroup(groupPeer: struct.GroupOutPeer)(implicit scope: TestScope): ResponseSeqDate = {
    val (r, _) = RequestLeaveGroup(groupPeer, rand.nextInt) :~> <~:[ResponseSeqDate]
    r
  }

  protected def editGroupTitle(groupPeer: struct.GroupOutPeer, title: String)(implicit scope: TestScope): ResponseSeqDate = {
    val (r, _) = RequestEditGroupTitle(
      groupOutPeer = groupPeer,
      randomId = rand.nextLong,
      title = title
    ) :~> <~:[ResponseSeqDate]
    r
  }

  protected def editGroupAvatar(groupPeer: struct.GroupOutPeer, fileLocation: models.FileLocation)(implicit scope: TestScope): ResponseEditGroupAvatar = {
    val (r, _) = RequestEditGroupAvatar(groupPeer, rand.nextLong, fileLocation) :~> <~:[ResponseEditGroupAvatar]
    r
  }

  protected def removeGroupAvatar(groupPeer: struct.GroupOutPeer)(implicit scope: TestScope): ResponseSeqDate = {
    val (r, _) = RequestRemoveGroupAvatar(groupPeer, rand.nextLong) :~> <~:[ResponseSeqDate]
    r
  }
}
