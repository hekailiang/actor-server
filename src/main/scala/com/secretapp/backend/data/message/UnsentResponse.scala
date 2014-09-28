package com.secretapp.backend.data.message

case class UnsentResponse(messageId: Long, requestMessageId: Long, length: Int) extends TransportMessage {
  override val header = UnsentResponse.header
}
object UnsentResponse extends TransportMessageMessageObject {
  override val header = 0x8
}
