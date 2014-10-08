package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupMessage(
  senderUID: Int,
  chatId: Int,
  keyHash: Long,
  aesKeyHash: BitVector,
  message: BitVector
) extends SeqUpdateMessage {
  val seqUpdateHeader = GroupMessage.seqUpdateHeader

  def userIds: Set[Int] = Set(senderUID)
}

object GroupMessage extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x14
}
