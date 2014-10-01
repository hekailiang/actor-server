package com.secretapp.backend.data.message

case class UnsentResponse(messageId: Long, requestMessageId: Long, length: Int) extends TransportMessage {
  val header = UnsentResponse.header
}

object UnsentResponse extends TransportMessageMessageObject {
  val header = 0x08
}
