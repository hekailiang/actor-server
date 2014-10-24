package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class GroupTitleChanged(groupId: Int, title: String) extends SeqUpdateMessage {
  override val header = GroupTitleChanged.header

  override def userIds: Set[Int] = Set()
}

object GroupTitleChanged extends SeqUpdateMessageObject {
  override val header = 0x26
}
