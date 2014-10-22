package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupUserAdded(
  chatId: Int,
  userId: Int,
  inviterUserId: Int
) extends SeqUpdateMessage {
  val header = GroupUserAdded.header

  def userIds: Set[Int] = Set(userId)
}

object GroupUserAdded extends SeqUpdateMessageObject {
  val header = 0x15
}
