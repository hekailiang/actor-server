package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.User


case class ContactRegistered(user: User) extends CommonUpdateMessage {
  override val commonUpdateType = ContactRegistered.commonUpdateType

  override def userIds: Set[Int] = Set(user.uid)
}

object ContactRegistered extends CommonUpdateMessageObject {
  override val commonUpdateType = 0x05
}
