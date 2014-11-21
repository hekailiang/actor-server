package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc.{RpcRequestMessage, RpcRequestMessageObject}

@SerialVersionUID(1L)
case class RequestAuthCodeCall(phoneNumber: Long, smsHash: String, appId: Int, apiKey: String) extends RpcRequestMessage {
  val header = RequestAuthCodeCall.header
}

object RequestAuthCodeCall extends RpcRequestMessageObject {
  val header = 0x5A
}
