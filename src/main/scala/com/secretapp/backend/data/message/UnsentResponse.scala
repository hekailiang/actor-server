package com.secretapp.backend.data.message

case class UnsentResponse(messageId : Long, requestMessageId : Long, length : Int) extends TransportMessage
object UnsentResponse {
  val header = 0x8
}
