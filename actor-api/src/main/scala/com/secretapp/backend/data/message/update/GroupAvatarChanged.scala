package com.secretapp.backend.data.message.update

import com.secretapp.backend.models

case class GroupAvatarChanged(groupId: Int, userId: Int, avatar: Option[models.Avatar], date: Long) extends SeqUpdateMessage {
  override val header = GroupAvatarChanged.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupAvatarChanged extends SeqUpdateMessageObject {
  val header = 0x27
}
