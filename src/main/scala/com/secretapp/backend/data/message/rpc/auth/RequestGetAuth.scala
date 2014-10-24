package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestGetAuth() extends RpcRequestMessage {
  val header = RequestGetAuth.header
}

object RequestGetAuth extends RpcRequestMessageObject {
  val header = 0x50
}
