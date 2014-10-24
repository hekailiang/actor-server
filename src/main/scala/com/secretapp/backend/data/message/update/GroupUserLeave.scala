package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupUserLeave(
  groupId: Int,
  userId: Int
) extends SeqUpdateMessage {
  val header = GroupUserLeave.header

  def userIds: Set[Int] = Set(userId)
}

object GroupUserLeave extends SeqUpdateMessageObject {
  val header = 0x17
}
