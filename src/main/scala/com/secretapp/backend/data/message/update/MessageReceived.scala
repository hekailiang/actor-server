package com.secretapp.backend.data.message.update

@SerialVersionUID(1l)
case class MessageReceived(uid: Int, randomId: Long) extends SeqUpdateMessage {
  val seqUpdateHeader = MessageReceived.seqUpdateHeader

  def userIds: Set[Int] = Set()
}

object MessageReceived extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x12
}
