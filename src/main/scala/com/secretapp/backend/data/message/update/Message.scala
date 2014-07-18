package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

case class Message(senderUID: Long,
                   destUID: Long,
                   mid: Int,
                   keyHash: Long,
                   useAesKey: Boolean,
                   aesKey: Option[BitVector],
                   message: BitVector) extends UpdateMessage
{
  val updateType = Message.updateType
}

object Message extends UpdateMessageObject {
  val updateType = 0x1
}
