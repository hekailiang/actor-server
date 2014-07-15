package com.secretapp.backend.data.message

case class NewSession(sessionId: Long, messageId: Long) extends TransportMessage
object NewSession extends TransportMessageMessageObject {
  val header = 0xc
}
