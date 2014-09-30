package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.User


case class ContactRegistered(userId: Int) extends SeqUpdateMessage {
  override val seqUpdateHeader = ContactRegistered.seqUpdateHeader

  override def userIds: Set[Int] = Set(userId)
}

object ContactRegistered extends SeqUpdateMessageObject {
  override val seqUpdateHeader = 0x05
}
