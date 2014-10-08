package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class InternalError(canTryAgain: Boolean, tryAgainDelay: Int) extends RpcResponse {
  val rpcType = InternalError.rpcType
}

object InternalError extends RpcResponseObject {
  val rpcType = 0x04
}
