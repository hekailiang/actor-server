package com.secretapp.backend.data.message.update

case class MessageRead(uid: Int, randomId: Long) extends CommonUpdateMessage {
  val commonUpdateType = MessageRead.commonUpdateType

  def userIds: Set[Int] = Set()
}

object MessageRead extends CommonUpdateMessageObject {
  val commonUpdateType = 0x13
}
