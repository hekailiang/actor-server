package com.secretapp.backend.data.message.rpc

case class Error(errorCode : Int, errorTag : String, userMessage : String, canTryAgain : Boolean) extends RpcResponse
object Error {
  val header = 0x2
}
