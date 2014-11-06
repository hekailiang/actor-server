package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class MessageSent(uid: Int, randomId: Long) extends SeqUpdateMessage {
  val header = MessageSent.header

  def userIds: Set[Int] = Set()
}

object MessageSent extends SeqUpdateMessageObject {
  val header = 0x4
}
