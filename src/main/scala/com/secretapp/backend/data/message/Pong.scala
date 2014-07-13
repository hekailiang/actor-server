package com.secretapp.backend.data.message

case class Pong(randomId : Long) extends TransportMessage
object Pong extends TransportMessageMessageObject {
  val header = 0x2
}
