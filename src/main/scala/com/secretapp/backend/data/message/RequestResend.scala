package com.secretapp.backend.data.message

case class RequestResend(messageId: Long) extends TransportMessage {
  val header = RequestResend.header
}

object RequestResend extends TransportMessageMessageObject {
  val header = 0x09
}
