package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupUserAdded(
  chatId: Int,
  userId: Int
) extends SeqUpdateMessage {
  val seqUpdateHeader = GroupUserAdded.seqUpdateHeader

  def userIds: Set[Int] = Set(userId)
}

object GroupUserAdded extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x15
}
