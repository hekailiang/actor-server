package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.User

case class UserChanged(user: User) extends CommonUpdateMessage {
  override val commonUpdateType = UserChanged.commonUpdateType

  override def userIds: Set[Int] = Set(user.uid)
}

object UserChanged extends CommonUpdateMessageObject {
  override val commonUpdateType = 0x36
}
