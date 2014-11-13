package com.secretapp.backend.data.message.update

import com.secretapp.backend.models

@SerialVersionUID(1L)
case class AvatarChanged(uid: Int, avatar: Option[models.Avatar]) extends SeqUpdateMessage {
  val header = AvatarChanged.header

  def userIds: Set[Int] = Set(uid)

  def groupIds: Set[Int] = Set.empty
}

object AvatarChanged extends SeqUpdateMessageObject {
  val header = 0x10
}
