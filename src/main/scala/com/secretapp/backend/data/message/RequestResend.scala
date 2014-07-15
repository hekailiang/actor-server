package com.secretapp.backend.data.message

case class RequestResend(messageId: Long) extends TransportMessage
object RequestResend extends TransportMessageMessageObject {
  val header = 0x9
}
