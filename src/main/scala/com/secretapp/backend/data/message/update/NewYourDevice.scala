package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

case class NewYourDevice(uid : Int, keyHash : Long, key : BitVector) extends UpdateMessage {
  val updateType = NewYourDevice.updateType
}

object NewYourDevice extends UpdateMessageObject {
  val updateType = 0x3
}
