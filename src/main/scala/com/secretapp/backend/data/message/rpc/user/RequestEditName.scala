package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

@SerialVersionUID(1L)
case class RequestEditName(name: String) extends RpcRequestMessage {
  val header = RequestEditName.requestType
}

object RequestEditName extends RpcRequestMessageObject {
  val requestType = 0x35
}
