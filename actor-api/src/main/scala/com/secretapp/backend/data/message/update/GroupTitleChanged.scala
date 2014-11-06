package com.secretapp.backend.data.message.update

case class GroupTitleChanged(groupId: Int, title: String) extends SeqUpdateMessage {
  val header = GroupTitleChanged.header

  def userIds: Set[Int] = Set()
}

object GroupTitleChanged extends SeqUpdateMessageObject {
  val header = 0x26
}
