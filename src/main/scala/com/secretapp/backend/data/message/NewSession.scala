package com.secretapp.backend.data.message

case class NewSession(sessionId : Long, messageId : Long) extends TransportMessage
object NewSession {
  val header = 0xc
}
