package com.secretapp.backend.data.message.update

case class GroupTitleChanged(groupId: Int, uid: Int, title: String, date: Long) extends SeqUpdateMessage {
  val header = GroupTitleChanged.header

  def userIds: Set[Int] = Set(uid)

  def groupIds: Set[Int] = Set(groupId)
}

object GroupTitleChanged extends SeqUpdateMessageObject {
  val header = 0x26
}
