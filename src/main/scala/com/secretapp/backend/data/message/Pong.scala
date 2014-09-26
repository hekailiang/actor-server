package com.secretapp.backend.data.message

case class Pong(randomId: Long) extends TransportMessage {
  override val header = Pong.header
}
object Pong extends TransportMessageMessageObject {
  override val header = 0x2
}
