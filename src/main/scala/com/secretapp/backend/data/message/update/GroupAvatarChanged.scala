package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class GroupAvatarChanged(groupId: Int, avatar: Option[Avatar]) extends SeqUpdateMessage {
  val header = GroupAvatarChanged.header

  def userIds: Set[Int] = Set()
}

object GroupAvatarChanged extends SeqUpdateMessageObject {
  val header = 0x27
}
