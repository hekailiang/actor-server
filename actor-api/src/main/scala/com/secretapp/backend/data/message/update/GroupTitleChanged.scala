package com.secretapp.backend.data.message.update

case class GroupTitleChanged(groupId: Int, randomId: Long, userId: Int, title: String, date: Long) extends SeqUpdateMessage {
  val header = GroupTitleChanged.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupTitleChanged extends SeqUpdateMessageObject {
  val header = 0x26
}
