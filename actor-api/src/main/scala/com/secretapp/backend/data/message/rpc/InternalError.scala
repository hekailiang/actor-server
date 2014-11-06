package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class InternalError(canTryAgain: Boolean, tryAgainDelay: Int) extends RpcResponse {
  val header = InternalError.header
}

object InternalError extends RpcResponseObject {
  val header = 0x04
}
