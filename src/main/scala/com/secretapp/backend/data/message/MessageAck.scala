package com.secretapp.backend.data.message

case class MessageAck(messageIds: Vector[Long]) extends TransportMessage {
  val header = MessageAck.header
}

object MessageAck extends TransportMessageMessageObject {
  val header = 0x06
}
