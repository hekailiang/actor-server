package com.secretapp.backend.data.message.update

import com.secretapp.backend.models

case class GroupAvatarChanged(groupId: Int, avatar: Option[models.Avatar]) extends SeqUpdateMessage {
  override val header = GroupAvatarChanged.header

  def userIds: Set[Int] = Set()
}

object GroupAvatarChanged extends SeqUpdateMessageObject {
  val header = 0x27
}
