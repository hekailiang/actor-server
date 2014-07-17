package com.secretapp.backend.data.message.rpc

case class Error(errorCode: Int, errorTag: String, userMessage: String, canTryAgain: Boolean = true) extends RpcResponse
object Error extends RpcResponseObject {
  val rpcType = 0x2
}
