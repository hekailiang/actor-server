package com.secretapp.backend.data.message.update

case class Typing(uid: Int, typingType: Int) extends WeakUpdateMessage {
  val weakUpdateHeader = Typing.weakUpdateHeader
}

object Typing extends WeakUpdateMessageObject {
  val weakUpdateHeader = 0x06
}
