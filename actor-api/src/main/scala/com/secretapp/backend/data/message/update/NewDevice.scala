package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class NewDevice(userId: Int, keyHash: Long, key: Option[BitVector], date: Long) extends SeqUpdateMessage {
  val header = NewDevice.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set.empty
}

object NewDevice extends SeqUpdateMessageObject {
  val header = 0x2
}
