package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

case class RequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String) extends RpcRequestMessage {
  override val header = RequestAuthCode.requestType
}
object RequestAuthCode extends RpcRequestMessageObject {
  override val requestType = 0x1
}
