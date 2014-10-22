package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class GroupTitleChanged(groupId: Int, title: String) extends SeqUpdateMessage {
  override val seqUpdateHeader = GroupTitleChanged.seqUpdateHeader

  override def userIds: Set[Int] = Set()
}

object GroupTitleChanged extends SeqUpdateMessageObject {
  override val seqUpdateHeader = 0x26
}
