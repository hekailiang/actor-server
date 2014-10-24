package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
import scala.collection.immutable
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class GroupInvite(
  groupId: Int,
  accessHash: Long,
  groupCreatorUserId: Int,
  inviterUserId: Int,
  title: String,
  users: immutable.Seq[UserId],
  invite: EncryptedRSAPackage
) extends SeqUpdateMessage {
  val header = GroupInvite.header

  def userIds: Set[Int] = Set(users: _*) map (_.uid)
}

object GroupInvite extends SeqUpdateMessageObject {
  val header = 0x19
}
