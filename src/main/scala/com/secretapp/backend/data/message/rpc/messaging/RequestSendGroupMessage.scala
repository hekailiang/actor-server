package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1l)
case class RequestSendGroupMessage(
  chatId: Int,
  accessHash: Long,
  randomId: Long,
  keyHash: BitVector,
  message: BitVector
) extends RpcRequestMessage {
  val header = RequestSendGroupMessage.requestType
}

object RequestSendGroupMessage extends RpcRequestMessageObject {
  val requestType = 0x43
}
