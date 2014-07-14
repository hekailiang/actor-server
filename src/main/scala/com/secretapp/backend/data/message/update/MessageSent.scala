package com.secretapp.backend.data.message.update

case class MessageSent(mid : Int, randomId : Long) extends UpdateMessage
object MessageSent extends UpdateMessageObject {
  val updateType = 0x4
}
