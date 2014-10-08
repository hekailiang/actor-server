package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Avatar

case class NameChanged(uid: Int, name: Option[String]) extends SeqUpdateMessage {
  override val seqUpdateHeader = NameChanged.seqUpdateHeader

  override def userIds: Set[Int] = Set(uid)
}

object NameChanged extends SeqUpdateMessageObject {
  override val seqUpdateHeader = 0x20
}
