package com.secretapp.backend.data.message

case class UnsentMessage(messageId : Long, length : Int) extends TransportMessage
object UnsentMessage extends TransportMessageMessageObject {
  val header = 0x7
}
