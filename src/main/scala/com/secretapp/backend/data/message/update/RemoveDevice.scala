package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RemoveDevice(uid: Int, keyHash: Long) extends SeqUpdateMessage {
  val seqUpdateHeader = RemoveDevice.seqUpdateHeader

  def userIds: Set[Int] = Set(uid)
}

object RemoveDevice extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x25
}
