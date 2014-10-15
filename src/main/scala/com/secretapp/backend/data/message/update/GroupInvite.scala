package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
import scala.collection.immutable
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class GroupInvite(
  chatId: Int,
  accessHash: Long,
  chatCreatorUserId: Int,
  title: String,
  users: immutable.Seq[UserId],
  invite: EncryptedRSAPackage
) extends SeqUpdateMessage {
  val seqUpdateHeader = GroupInvite.seqUpdateHeader

  def userIds: Set[Int] = Set(users: _*) map (_.uid)
}

object GroupInvite extends SeqUpdateMessageObject {
  val seqUpdateHeader = 0x19
}
