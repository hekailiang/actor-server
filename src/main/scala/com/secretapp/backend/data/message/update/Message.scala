package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
import scodec.bits.BitVector

case class Message(senderUID: Int,
                   destUID: Int,
                   message: EncryptedRSAPackage) extends SeqUpdateMessage
{
  val seqUpdateHeader = Message.seqUpdateHeader

  def userIds: Set[Int] = Set(senderUID, destUID)
}

object Message extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x1
}
