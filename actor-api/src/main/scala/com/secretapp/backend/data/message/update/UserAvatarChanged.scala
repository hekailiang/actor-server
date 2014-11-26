package com.secretapp.backend.data.message.update

import com.secretapp.backend.models

@SerialVersionUID(1L)
case class UserAvatarChanged(userId: Int, avatar: Option[models.Avatar]) extends SeqUpdateMessage {
  val header = UserAvatarChanged.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set.empty
}

object UserAvatarChanged extends SeqUpdateMessageObject {
  val header = 0x10
}
