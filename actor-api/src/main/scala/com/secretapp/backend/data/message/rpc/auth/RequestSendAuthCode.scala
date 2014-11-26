package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestSendAuthCode(phoneNumber: Long, appId: Int, apiKey: String) extends RpcRequestMessage {
  val header = RequestSendAuthCode.header
}

object RequestSendAuthCode extends RpcRequestMessageObject {
  val header = 0x01
}
