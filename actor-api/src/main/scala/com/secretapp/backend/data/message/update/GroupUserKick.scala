package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class GroupUserKick(
  groupId: Int,
  userId: Int,
  kickerUid: Int,
  date: Long
) extends SeqUpdateMessage {
  val header = GroupUserKick.header

  def userIds: Set[Int] = Set(userId)
}

object GroupUserKick extends SeqUpdateMessageObject {
  val header = 0x18
}
