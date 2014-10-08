package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestInviteUser(
  chatId: Int,
  accessHash: Long,
  userId: Int,
  userAccessHash: Long,
  randomId: Long,
  chatKeyHash: BitVector,
  invite: immutable.Seq[EncryptedMessage]
) extends RpcRequestMessage {
  val header = RequestInviteUser.requestType
}

object RequestInviteUser extends RpcRequestMessageObject {
  val requestType = 0x45
}
