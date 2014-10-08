package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
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
