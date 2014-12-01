package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class GroupInvite(
  groupId: Int,
  randomId: Long,
  inviterUserId: Int,
  date: Long
) extends SeqUpdateMessage {
  val header = GroupInvite.header

  def userIds: Set[Int] = Set(inviterUserId)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupInvite extends SeqUpdateMessageObject {
  val header = 0x24
}
