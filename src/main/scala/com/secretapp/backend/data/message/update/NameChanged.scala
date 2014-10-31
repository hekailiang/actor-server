package com.secretapp.backend.data.message.update

case class NameChanged(uid: Int, name: Option[String]) extends SeqUpdateMessage {
  val header = NameChanged.header

  def userIds: Set[Int] = Set(uid)
}

object NameChanged extends SeqUpdateMessageObject {
  val header = 0x20
}
