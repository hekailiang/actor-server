package com.secretapp.backend.data.message

@SerialVersionUID(1l)
case class UnsentMessage(messageId: Long, length: Int) extends TransportMessage {
  val header = UnsentMessage.header
}

object UnsentMessage extends TransportMessageMessageObject {
  val header = 0x07
}
