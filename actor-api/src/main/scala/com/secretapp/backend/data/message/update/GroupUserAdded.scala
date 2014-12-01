package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class GroupUserAdded(
  groupId: Int,
  randomId: Long,
  userId: Int,
  inviterUserId: Int,
  date: Long
) extends SeqUpdateMessage {
  val header = GroupUserAdded.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupUserAdded extends SeqUpdateMessageObject {
  val header = 0x15
}
