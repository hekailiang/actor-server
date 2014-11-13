package com.secretapp.backend.data.message.update

import com.secretapp.backend.models

case class GroupAvatarChanged(groupId: Int, uid: Int, avatar: Option[models.Avatar], date: Long) extends SeqUpdateMessage {
  override val header = GroupAvatarChanged.header

  def userIds: Set[Int] = Set(uid)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupAvatarChanged extends SeqUpdateMessageObject {
  val header = 0x27
}
