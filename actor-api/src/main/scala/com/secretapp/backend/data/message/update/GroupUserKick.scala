package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class GroupUserKick(
  groupId: Int,
  randomId: Long,
  userId: Int,
  kickerUid: Int,
  date: Long
) extends SeqUpdateMessage {
  val header = GroupUserKick.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupUserKick extends SeqUpdateMessageObject {
  val header = 0x18
}
