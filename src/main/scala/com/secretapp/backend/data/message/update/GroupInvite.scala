package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupInvite(
  chatId: Int,
  accessHash: Long,
  title: String,
  users: immutable.Seq[UserId],
  keyHash: Long,
  aesEncryptedKey: BitVector,
  message: BitVector
) extends SeqUpdateMessage {
  val seqUpdateHeader = GroupInvite.seqUpdateHeader

  def userIds: Set[Int] = Set(users: _*) map (_.uid)
}

object GroupInvite extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x19
}
