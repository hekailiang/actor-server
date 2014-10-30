package com.secretapp.backend.data.message.update

case class NameChanged(uid: Int, name: Option[String]) extends SeqUpdateMessage {
  override val header = NameChanged.header

  override def userIds: Set[Int] = Set(uid)
}

object NameChanged extends SeqUpdateMessageObject {
  override val header = 0x20
}
