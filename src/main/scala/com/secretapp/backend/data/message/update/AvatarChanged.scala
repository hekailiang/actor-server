package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

@SerialVersionUID(1l)
case class AvatarChanged(uid: Int, avatar: Option[Avatar]) extends SeqUpdateMessage {
  val seqUpdateHeader = AvatarChanged.seqUpdateHeader

  def userIds: Set[Int] = Set(uid)
}

object AvatarChanged extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x10
}
