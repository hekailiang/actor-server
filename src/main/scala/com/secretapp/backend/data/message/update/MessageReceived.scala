package com.secretapp.backend.data.message.update

case class MessageReceived(uid: Int, randomId: Long) extends CommonUpdateMessage {
  val commonUpdateType = MessageReceived.commonUpdateType

  def userIds: Set[Int] = Set()
}

object MessageReceived extends CommonUpdateMessageObject {
  val commonUpdateType = 0x12
}
