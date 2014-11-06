package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupUserKick(
  groupId: Int,
  userId: Int,
  kickerUserId: Int
) extends SeqUpdateMessage {
  val header = GroupUserKick.header

  def userIds: Set[Int] = Set(userId)
}

object GroupUserKick extends SeqUpdateMessageObject {
  val header = 0x18
}
