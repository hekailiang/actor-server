package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.User


case class ContactRegistered(userId: Int) extends SeqUpdateMessage {
  val seqUpdateHeader = ContactRegistered.seqUpdateHeader

  def userIds: Set[Int] = Set(userId)
}

object ContactRegistered extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x05
}
