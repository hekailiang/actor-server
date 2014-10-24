package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.rpc.messaging.EncryptedAESPackage
import scala.collection.immutable
import scodec.bits.BitVector

case class GroupMessage(
  senderUID: Int,
  groupId: Int,
  message: EncryptedAESPackage
) extends SeqUpdateMessage {
  val header = GroupMessage.header

  def userIds: Set[Int] = Set(senderUID)
}

object GroupMessage extends SeqUpdateMessageObject {
  val header = 0x14
}
