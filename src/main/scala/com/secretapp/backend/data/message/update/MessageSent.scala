package com.secretapp.backend.data.message.update

case class MessageSent(mid: Int, randomId: Long) extends CommonUpdateMessage {
  val commonUpdateType = MessageSent.commonUpdateType

  def userIds: Set[Int] = Set()
}

object MessageSent extends CommonUpdateMessageObject {
  val commonUpdateType = 0x4
}
