package com.secretapp.backend.data.message

case class Drop(messageId: Long, message: String) extends TransportMessage {
  override val header = Drop.header
}
object Drop extends TransportMessageMessageObject {
  override val header = 0xd
}
