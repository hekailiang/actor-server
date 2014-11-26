package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestSendAuthCall(phoneNumber: Long, smsHash: String, appId: Int, apiKey: String) extends RpcRequestMessage {
  val header = RequestSendAuthCall.header
}

object RequestSendAuthCall extends RpcRequestMessageObject {
  val header = 0x5A
}
