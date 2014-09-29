package com.secretapp.backend.data.message.rpc

case class InternalError(canTryAgain: Boolean, tryAgainDelay: Int) extends RpcResponse {
  override val rpcType = InternalError.rpcType
}
object InternalError extends RpcResponseObject {
  override val rpcType = 0x4
}
