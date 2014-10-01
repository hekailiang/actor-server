package com.secretapp.backend.data.message

case class Pong(randomId: Long) extends TransportMessage {
  val header = Pong.header
}

object Pong extends TransportMessageMessageObject {
  val header = 0x02
}
