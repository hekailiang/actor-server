package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class AvatarChanged(uid: Int, avatar: Option[Avatar]) extends CommonUpdateMessage {
  val commonUpdateType = AvatarChanged.commonUpdateType

  def userIds: Set[Int] = Set(uid)
}

object AvatarChanged extends CommonUpdateMessageObject {
  val commonUpdateType = 0x09
}
