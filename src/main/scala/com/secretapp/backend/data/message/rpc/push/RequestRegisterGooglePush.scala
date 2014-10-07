package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

@SerialVersionUID(1l)
case class RequestRegisterGooglePush(projectId: Long, token: String) extends RpcRequestMessage {
  val header = RequestRegisterGooglePush.requestType
}

object RequestRegisterGooglePush extends RpcRequestMessageObject {
  val requestType = 0x33
}
