package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RemovedDevice(userId: Int, keyHash: Long) extends SeqUpdateMessage {
  val header = RemovedDevice.header

  def userIds: Set[Int] = Set(userId)

  def groupIds: Set[Int] = Set.empty
}

object RemovedDevice extends SeqUpdateMessageObject {
  val header = 0x25
}
