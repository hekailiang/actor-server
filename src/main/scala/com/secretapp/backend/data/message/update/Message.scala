package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

case class Message(senderUID : Int,
                   destUID : Int,
                   mid : Int,
                   keyHash : Long,
                   useAesKey : Boolean,
                   aesKey : Option[BitVector],
                   message : BitVector) extends UpdateMessage
object Message extends UpdateMessageObject {
  val updateType = 0x1
}
