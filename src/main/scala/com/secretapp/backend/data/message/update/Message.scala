package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

case class Message(senderUID: Int,
                   destUID: Int,
                   mid: Int,
                   keyHash: Long,
                   useAesKey: Boolean,
                   aesKey: Option[BitVector],
                   message: BitVector) extends CommonUpdateMessage
{
  val commonUpdateType = Message.commonUpdateType

  def userIds: Set[Int] = Set(senderUID, destUID)
}

object Message extends CommonUpdateMessageObject {
  val commonUpdateType = 0x1
}
