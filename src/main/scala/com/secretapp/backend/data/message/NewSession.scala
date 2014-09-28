package com.secretapp.backend.data.message

case class NewSession(sessionId: Long, messageId: Long) extends TransportMessage {
  override val header = NewSession.header
}
object NewSession extends TransportMessageMessageObject {
  override val header = 0xc
}
