package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

case class RequestSendMessage(uid: Int,
                              accessHash: Long,
                              randomId: Long,
                              message: EncryptedMessage,
                              selfMessage: Option[EncryptedMessage]) extends RpcRequestMessage {
  override val header = RequestSendMessage.requestType
}

object RequestSendMessage extends RpcRequestMessageObject {
  override val requestType = 0x0e
}
