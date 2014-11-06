package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String) extends RpcRequestMessage {
  val header = RequestAuthCode.header
}

object RequestAuthCode extends RpcRequestMessageObject {
  val header = 0x01
}
