package com.secretapp.backend.data.message.update

case class NewDevice(uid : Int, keyHash : Long) extends UpdateMessage {
  val updateType = NewDevice.updateType
}

object NewDevice extends UpdateMessageObject {
  val updateType = 0x2
}
