package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestSendGroupMessage(
  chatId: Int,
  accessHash: Long,
  randomId: Long,
  message: EncryptedAESMessage
) extends RpcRequestMessage {
  val header = RequestSendGroupMessage.header
}


object RequestSendGroupMessage extends RpcRequestMessageObject {
  val header = 0x43
}
