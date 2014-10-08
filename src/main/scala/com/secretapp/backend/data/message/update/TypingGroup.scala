package com.secretapp.backend.data.message.update

case class TypingGroup(chatId: Int, uid: Int, typingType: Int) extends WeakUpdateMessage {
  val weakUpdateHeader = TypingGroup.weakUpdateHeader
}

object TypingGroup extends WeakUpdateMessageObject {
  val weakUpdateHeader = 0x22
}
