package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class AvatarChanged(uid: Int, avatar: Option[Avatar]) extends SeqUpdateMessage {
  override val seqUpdateHeader = AvatarChanged.seqUpdateHeader

  override def userIds: Set[Int] = Set(uid)
}

object AvatarChanged extends SeqUpdateMessageObject {
  override val seqUpdateHeader = 0x10
}
