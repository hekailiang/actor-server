package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

case class RequestRegisterGooglePush(projectId: Long, token: String) extends RpcRequestMessage {
  override val header = RequestRegisterGooglePush.requestType
}
object RequestRegisterGooglePush extends RpcRequestMessageObject {
  override val requestType = 0x33
}
