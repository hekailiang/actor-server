package com.secretapp.backend.data.message.update

import scodec.bits.BitVector

case class Message(senderUID: Int,
                   destUID: Int,
                   keyHash: Long,
                   aesEncryptedKey: BitVector,
                   message: BitVector) extends SeqUpdateMessage
{
  val seqUpdateHeader = Message.seqUpdateHeader

  def userIds: Set[Int] = Set(senderUID, destUID)
}

object Message extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x1
}
