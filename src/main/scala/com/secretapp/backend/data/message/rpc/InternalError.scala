package com.secretapp.backend.data.message.rpc

case class InternalError(canTryAgaint : Boolean, tryAgainDelay : Int) extends RpcResponse
object InternalError {
  val header = 0x4
}
