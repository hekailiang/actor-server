package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class ConnectionNotInitedError() extends RpcResponse {
  val rpcType = ConnectionNotInitedError.rpcType
}

object ConnectionNotInitedError extends RpcResponseObject {
  val rpcType = 0x5
}
