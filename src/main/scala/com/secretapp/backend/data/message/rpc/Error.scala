package com.secretapp.backend.data.message.rpc

case class Error(code: Int, tag: String, userMessage: String, canTryAgain: Boolean = false) extends RpcResponse {
  override val rpcType = Error.rpcType
}
object Error extends RpcResponseObject {
  override val rpcType = 0x2
}
