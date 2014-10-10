package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.rpc.messaging.EncryptedAESPackage
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupMessage(
  senderUID: Int,
  chatId: Int,
  message: EncryptedAESPackage
) extends SeqUpdateMessage {
  val seqUpdateHeader = GroupMessage.seqUpdateHeader

  def userIds: Set[Int] = Set(senderUID)
}

object GroupMessage extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x14
}
