package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class Message(senderUID: Int,
                   destUID: Int,
                   message: EncryptedRSAPackage) extends SeqUpdateMessage
{
  val header = Message.header

  def userIds: Set[Int] = Set(senderUID, destUID)
}

object Message extends SeqUpdateMessageObject {
  val header = 0x1
}
