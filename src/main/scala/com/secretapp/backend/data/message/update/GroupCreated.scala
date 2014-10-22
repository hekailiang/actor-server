package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
import scala.collection.immutable
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class GroupCreated(
  chatId: Int,
  accessHash: Long,
  title: String,
  invite: EncryptedRSAPackage
) extends SeqUpdateMessage {
  val header = GroupCreated.header

  def userIds: Set[Int] = Set()
}

object GroupCreated extends SeqUpdateMessageObject {
  val header = 0x24
}
