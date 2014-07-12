package com.secretapp.backend.data.message

case class Drop(messageId : Long, message : String) extends TransportMessage
object Drop {
  val header = 0xd
}
