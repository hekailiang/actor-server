package com.secretapp.backend.data.message.rpc

case class ConnectionNotInitedError() extends RpcResponse
object ConnectionNotInitedError extends RpcResponseObject {
  val rpcType = 0x5
}
