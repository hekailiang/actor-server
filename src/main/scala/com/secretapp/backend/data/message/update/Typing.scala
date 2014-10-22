package com.secretapp.backend.data.message.update

case class Typing(uid: Int, typingType: Int) extends WeakUpdateMessage {
  val header = Typing.header
}

object Typing extends WeakUpdateMessageObject {
  val header = 0x06
}
