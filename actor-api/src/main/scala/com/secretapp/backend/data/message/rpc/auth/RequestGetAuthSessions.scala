package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestGetAuthSessions() extends RpcRequestMessage {
  val header = RequestGetAuthSessions.header
}

object RequestGetAuthSessions extends RpcRequestMessageObject {
  val header = 0x50
}
