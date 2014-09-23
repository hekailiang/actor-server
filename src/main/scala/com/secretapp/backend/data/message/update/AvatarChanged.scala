package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class AvatarChanged(uid: Int, avatar: Option[Avatar]) extends CommonUpdateMessage {
  override val commonUpdateType = AvatarChanged.commonUpdateType

  override def userIds: Set[Int] = Set(uid)
}

object AvatarChanged extends CommonUpdateMessageObject {
  override val commonUpdateType = 0x10
}
