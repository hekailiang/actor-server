package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

@SerialVersionUID(1L)
case class RequestRegisterGooglePush(projectId: Long, token: String) extends RpcRequestMessage {
  val header = RequestRegisterGooglePush.header
}

object RequestRegisterGooglePush extends RpcRequestMessageObject {
  val header = 0x33
}
