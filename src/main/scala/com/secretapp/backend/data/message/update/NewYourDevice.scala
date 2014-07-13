package com.secretapp.backend.data.message.update

case class NewYourDevice(uid : Int, keyHash : Long, key : List[Byte]) extends UpdateMessage
object NewYourDevice extends UpdateMessageObject {
  val updateType = 0x3
}
