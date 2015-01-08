package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class PhoneTitleChanged(
  phoneId: Int,
  title: String
) extends SeqUpdateMessage {
  val header = PhoneTitleChanged.header

  def userIds: Set[Int] = Set.empty

  def groupIds: Set[Int] = Set.empty
}

object PhoneTitleChanged extends SeqUpdateMessageObject {
  val header = 0x59
}
