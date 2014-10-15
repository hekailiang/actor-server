package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestSendMessage(uid: Int,
                              accessHash: Long,
                              randomId: Long,
                              message: EncryptedRSAMessage) extends RpcRequestMessage {
  val header = RequestSendMessage.requestType
}

object RequestSendMessage extends RpcRequestMessageObject {
  val requestType = 0x0e
}
