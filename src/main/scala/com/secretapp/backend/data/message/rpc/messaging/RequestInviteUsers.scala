package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestInviteUsers(
  groupId: Int,
  accessHash: Long,
  randomId: Long,
  groupKeyHash: BitVector,
  broadcast: EncryptedRSABroadcast
) extends RpcRequestMessage {
  val header = RequestInviteUsers.requestType
}


object RequestInviteUsers extends RpcRequestMessageObject {
  val requestType = 0x45
}
