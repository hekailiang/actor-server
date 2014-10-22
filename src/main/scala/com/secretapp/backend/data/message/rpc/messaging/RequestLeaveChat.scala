package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestLeaveChat(
  chatId: Int,
  accessHash: Long
) extends RpcRequestMessage {
  val header = RequestLeaveChat.header
}

object RequestLeaveChat extends RpcRequestMessageObject {
  val header = 0x46
}
