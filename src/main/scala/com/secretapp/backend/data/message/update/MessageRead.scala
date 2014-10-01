package com.secretapp.backend.data.message.update

case class MessageRead(uid: Int, randomId: Long) extends SeqUpdateMessage {
  val seqUpdateHeader = MessageRead.seqUpdateHeader

  def userIds: Set[Int] = Set()
}

object MessageRead extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x13
}
