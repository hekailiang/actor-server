package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestTerminateAllSessions() extends RpcRequestMessage {
  val header = RequestTerminateAllSessions.header
}

object RequestTerminateAllSessions extends RpcRequestMessageObject {
  val header = 0x53
}
