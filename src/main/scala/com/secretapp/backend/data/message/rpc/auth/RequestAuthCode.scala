package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

case class RequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String) extends RpcRequestMessage {
  val header = RequestAuthCode.requestType
}

object RequestAuthCode extends RpcRequestMessageObject {
  val requestType = 0x01
}
