package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class ConnectionNotInitedError() extends RpcResponse {
  val header = ConnectionNotInitedError.header
}

object ConnectionNotInitedError extends RpcResponseObject {
  val header = 0x05
}
