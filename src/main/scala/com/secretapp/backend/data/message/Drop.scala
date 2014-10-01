package com.secretapp.backend.data.message

case class Drop(messageId: Long, message: String) extends TransportMessage {
  val header = Drop.header
}

object Drop extends TransportMessageMessageObject {
  val header = 0xd
}
