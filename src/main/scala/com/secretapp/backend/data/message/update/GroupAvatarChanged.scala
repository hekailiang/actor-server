package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class GroupAvatarChanged(groupId: Int, avatar: Option[Avatar]) extends SeqUpdateMessage {
  override val seqUpdateHeader = GroupAvatarChanged.seqUpdateHeader

  override def userIds: Set[Int] = Set()
}

object GroupAvatarChanged extends SeqUpdateMessageObject {
  override val seqUpdateHeader = 0x27
}
