package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

@SerialVersionUID(1L)
case class NewFullDevice(uid: Int, keyHash: Long, key: BitVector) extends SeqUpdateMessage {
  val seqUpdateHeader = NewFullDevice.seqUpdateHeader

  def userIds: Set[Int] = Set(uid)
}

object NewFullDevice extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x3
}
