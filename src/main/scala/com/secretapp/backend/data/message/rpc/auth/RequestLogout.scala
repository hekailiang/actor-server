package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestLogout() extends RpcRequestMessage {
  val header = RequestLogout.requestType
}

object RequestLogout extends RpcRequestMessageObject {
  val requestType = 0x54
}
