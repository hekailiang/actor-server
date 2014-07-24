package com.secretapp.backend.data.message.update

case class NewDevice(uid: Int, keyHash: Long) extends CommonUpdateMessage {
  val commonUpdateType = NewDevice.commonUpdateType
}

object NewDevice extends CommonUpdateMessageObject {
  val commonUpdateType = 0x2
}
