package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class MessageReceived(uid: Int, randomId: Long) extends SeqUpdateMessage {
  val header = MessageReceived.header

  def userIds: Set[Int] = Set()
}

object MessageReceived extends SeqUpdateMessageObject {
  val header = 0x12
}
