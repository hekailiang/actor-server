package com.secretapp.backend.data.message.rpc.messaging

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1l)
case class RequestLeaveChat(
  chatId: Int,
  accessHash: Long
) extends RpcRequestMessage {
  val header = RequestLeaveChat.requestType
}

object RequestLeaveChat extends RpcRequestMessageObject {
  val requestType = 0x46
}
