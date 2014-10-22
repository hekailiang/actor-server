package com.secretapp.backend.data.message.update

case class GroupOnline(chatId: Int, count: Int) extends WeakUpdateMessage {
  val header = GroupOnline.header
}

object GroupOnline extends WeakUpdateMessageObject {
  val header = 0x21
}
