package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestInviteUsers(
  chatId: Int,
  accessHash: Long,
  randomId: Long,
  chatKeyHash: BitVector,
  broadcast: EncryptedRSABroadcast
) extends RpcRequestMessage

object RequestInviteUsers extends RpcRequestMessage {
  val requestType = 0x45
}
