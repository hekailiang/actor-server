package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class NewDevice(uid: Int, keyHash: Long) extends SeqUpdateMessage {
  val header = NewDevice.header

  def userIds: Set[Int] = Set(uid)
}

object NewDevice extends SeqUpdateMessageObject {
  val header = 0x2
}
