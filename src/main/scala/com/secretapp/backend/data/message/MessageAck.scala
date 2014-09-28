package com.secretapp.backend.data.message

case class MessageAck(messageIds: Vector[Long]) extends TransportMessage {
  override val header = MessageAck.header
}
object MessageAck extends TransportMessageMessageObject {
  override val header = 0x6
}
