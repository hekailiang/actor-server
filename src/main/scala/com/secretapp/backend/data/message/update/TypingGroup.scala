package com.secretapp.backend.data.message.update

@SerialVersionUID(1L)
case class TypingGroup(chatId: Int, uid: Int, typingType: Int) extends WeakUpdateMessage {
  val header = TypingGroup.header
}

object TypingGroup extends WeakUpdateMessageObject {
  val header = 0x22
}
