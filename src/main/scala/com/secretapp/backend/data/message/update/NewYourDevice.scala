package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class NewYourDevice(uid: Int, keyHash: Long, key: BitVector) extends SeqUpdateMessage {
  val seqUpdateHeader = NewYourDevice.seqUpdateHeader

  def userIds: Set[Int] = Set()
}

object NewYourDevice extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x3
}
