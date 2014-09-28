package com.secretapp.backend.data.message

case class UnsentMessage(messageId: Long, length: Int) extends TransportMessage {
  override val header = UnsentMessage.header
}
object UnsentMessage extends TransportMessageMessageObject {
  override val header = 0x7
}
