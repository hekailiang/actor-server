package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class MessageRead(uid: Int, randomId: Long) extends SeqUpdateMessage {
  val header = MessageRead.header

  def userIds: Set[Int] = Set()
}

object MessageRead extends SeqUpdateMessageObject {
  val header = 0x13
}
