package com.secretapp.backend.data.message.update

case class Message(senderUID : Int,
                   destUID : Int,
                   mid : Int,
                   keyHash : Long,
                   useAesKey : Boolean,
                   aesKey : Option[List[Byte]],
                   message : List[Byte]) extends UpdateMessage
object Message extends UpdateMessageObject {
  val updateType = 0x1
}
