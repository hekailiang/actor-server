package com.secretapp.backend.data.message

case class Ping(randomId: Long) extends TransportMessage {
  override val header = Ping.header
}
object Ping extends TransportMessageMessageObject {
  val header = 0x1
}
