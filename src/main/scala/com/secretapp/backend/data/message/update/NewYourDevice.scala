package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

case class NewYourDevice(uid: Int, keyHash: Long, key: BitVector) extends CommonUpdateMessage {
  val commonUpdateType = NewYourDevice.commonUpdateType

  def userIds: Set[Int] = Set()
}

object NewYourDevice extends CommonUpdateMessageObject {
  val commonUpdateType = 0x3
}
