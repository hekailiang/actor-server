package com.secretapp.backend.data.message.rpc

case class ConnectionNotInitedError() extends RpcResponse {
  override val rpcType = ConnectionNotInitedError.rpcType
}
object ConnectionNotInitedError extends RpcResponseObject {
  override val rpcType = 0x5
}
