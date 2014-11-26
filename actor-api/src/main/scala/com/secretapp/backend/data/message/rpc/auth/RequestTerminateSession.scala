package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestTerminateSession(id: Int) extends RpcRequestMessage {
  val header = RequestTerminateSession.header
}

object RequestTerminateSession extends RpcRequestMessageObject {
  val header = 0x52
}
