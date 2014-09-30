package com.secretapp.backend.data.message.update

case class MessageSent(uid: Int, randomId: Long) extends SeqUpdateMessage {
  val seqUpdateHeader = MessageSent.seqUpdateHeader

  def userIds: Set[Int] = Set()
}

object MessageSent extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x4
}
