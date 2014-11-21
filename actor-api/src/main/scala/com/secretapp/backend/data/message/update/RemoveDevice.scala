package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RemoveDevice(userId: Int, keyHash: Long) extends SeqUpdateMessage {
  val header = RemoveDevice.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set.empty
}

object RemoveDevice extends SeqUpdateMessageObject {
  val header = 0x25
}
