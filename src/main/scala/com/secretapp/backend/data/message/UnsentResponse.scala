package com.secretapp.backend.data.message

case class UnsentResponse(messageId: Long, requestMessageId: Long, length: Int) extends TransportMessage
object UnsentResponse extends TransportMessageMessageObject {
  val header = 0x8
}
