package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestInviteUsers(
  chatId: Int,
  accessHash: Long,
  randomId: Long,
  chatKeyHash: BitVector,
  broadcast: EncryptedRSABroadcast
) extends RpcRequestMessage {
  val header = RequestInviteUsers.header
}


object RequestInviteUsers extends RpcRequestMessageObject {
  val header = 0x45
}
