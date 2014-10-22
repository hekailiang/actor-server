package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class ContactRegistered(userId: Int) extends SeqUpdateMessage {
  val header = ContactRegistered.header

  def userIds: Set[Int] = Set(userId)
}

object ContactRegistered extends SeqUpdateMessageObject {
  val header = 0x05
}
