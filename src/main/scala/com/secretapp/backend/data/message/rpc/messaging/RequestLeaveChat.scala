package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestLeaveChat(
  chatId: Int,
  accessHash: Long
) extends RpcRequestMessage

object RequestLeaveChat extends RpcRequestMessage {
  val requestType = 0x46
}
