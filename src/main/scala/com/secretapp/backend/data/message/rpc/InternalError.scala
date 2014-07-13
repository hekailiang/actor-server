package com.secretapp.backend.data.message.rpc

case class InternalError(canTryAgaint : Boolean, tryAgainDelay : Int) extends RpcResponse
object InternalError extends RpcResponseObject {
  val rpcType = 0x4
}
