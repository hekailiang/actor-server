package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class GroupUserLeave(groupId: Int, userId: Int, date: Long) extends SeqUpdateMessage {
  val header = GroupUserLeave.header

  def userIds: Set[Int] = Set(userId)
}

object GroupUserLeave extends SeqUpdateMessageObject {
  val header = 0x17
}
