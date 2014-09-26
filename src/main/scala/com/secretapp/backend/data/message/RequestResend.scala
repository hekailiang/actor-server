package com.secretapp.backend.data.message

case class RequestResend(messageId: Long) extends TransportMessage {
  override val header = RequestResend.header
}
object RequestResend extends TransportMessageMessageObject {
  override val header = 0x9
}
