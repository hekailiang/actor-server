package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

case class RequestSendGroupMessage(
  chatId: Int,
  accessHash: Long,
  randomId: Long,
  message: EncryptedMessage,
  selfMessage: Option[EncryptedMessage]
) extends RpcRequestMessage {
  override val header = RequestSendGroupMessage.requestType
}

object RequestSendGroupMessage extends RpcRequestMessageObject {
  override val requestType = 0x43
}
