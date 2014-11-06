package com.secretapp.backend.data.message

@SerialVersionUID(1L)
case class Ping(randomId: Long) extends TransportMessage {
  val header = Ping.header
}

object Ping extends TransportMessageMessageObject {
  val header = 0x01
}
