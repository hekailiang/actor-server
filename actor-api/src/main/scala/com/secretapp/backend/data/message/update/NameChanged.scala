package com.secretapp.backend.data.message.update

case class NameChanged(userId: Int, name: Option[String]) extends SeqUpdateMessage {
  val header = NameChanged.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set.empty
}

object NameChanged extends SeqUpdateMessageObject {
  val header = 0x20
}
