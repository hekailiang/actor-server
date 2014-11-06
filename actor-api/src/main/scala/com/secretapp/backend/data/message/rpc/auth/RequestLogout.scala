package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestLogout() extends RpcRequestMessage {
  val header = RequestLogout.header
}

object RequestLogout extends RpcRequestMessageObject {
  val header = 0x54
}
