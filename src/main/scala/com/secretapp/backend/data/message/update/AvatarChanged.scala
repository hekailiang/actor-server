package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

@SerialVersionUID(1L)
case class AvatarChanged(uid: Int, avatar: Option[Avatar]) extends SeqUpdateMessage {
  val header = AvatarChanged.header

  def userIds: Set[Int] = Set(uid)
}

object AvatarChanged extends SeqUpdateMessageObject {
  val header = 0x10
}
