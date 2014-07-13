package com.secretapp.backend.data.message

case class Drop(messageId : Long, message : String) extends TransportMessage
object Drop extends TransportMessageMessageObject {
  val header = 0xd
}
