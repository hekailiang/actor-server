package com.secretapp.backend.data.message

@SerialVersionUID(1l)
case class NewSession(sessionId: Long, messageId: Long) extends TransportMessage {
  val header = NewSession.header
}

object NewSession extends TransportMessageMessageObject {
  val header = 0x0C
}
