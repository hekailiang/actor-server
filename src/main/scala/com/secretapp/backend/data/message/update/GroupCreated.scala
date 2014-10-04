package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.rpc.messaging.InviteUser
import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupCreated(
  randomId: Long,
  chatId: Int,
  accessHash: Long,
  title: String,
  keyHash: BitVector,
  invites: immutable.Seq[InviteUser]
) extends SeqUpdateMessage {
  val seqUpdateHeader = GroupCreated.seqUpdateHeader

  def userIds: Set[Int] = Set()
}

object GroupCreated extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x24
}
