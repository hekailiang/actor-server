package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestRemoveUser(
  chatId: Int,
  accessHash: Long,
  userId: Int,
  userAccessHash: Long
) extends RpcRequestMessage

object RequestRemoveUser extends RpcRequestMessage {
  val requestType = 0x47
}
