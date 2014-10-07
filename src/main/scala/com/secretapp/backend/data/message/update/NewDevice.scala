package com.secretapp.backend.data.message.update

@SerialVersionUID(1l)
case class NewDevice(uid: Int, keyHash: Long) extends SeqUpdateMessage {
  val seqUpdateHeader = NewDevice.seqUpdateHeader

  def userIds: Set[Int] = Set(uid)
}

object NewDevice extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x2
}
