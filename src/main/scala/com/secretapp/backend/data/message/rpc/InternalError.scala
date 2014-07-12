package com.secretapp.backend.data.message.rpc

case class InternalError(canTryAgaint : Boolean, tryAgainDelay : Int) extends RpcMessage
object InternalError {
  val header = 0x4
}
