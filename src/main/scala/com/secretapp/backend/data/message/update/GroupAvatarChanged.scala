package com.secretapp.backend.data.message.update

import com.secretapp.backend.models

case class GroupAvatarChanged(groupId: Int, avatar: Option[models.Avatar]) extends SeqUpdateMessage {
  override val header = GroupAvatarChanged.header

  override def userIds: Set[Int] = Set()
}

object GroupAvatarChanged extends SeqUpdateMessageObject {
  override val header = 0x27
}
