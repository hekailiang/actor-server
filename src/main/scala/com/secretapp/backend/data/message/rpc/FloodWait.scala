package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class FloodWait(delay: Int) extends RpcResponse {
  val header = FloodWait.header
}

object FloodWait extends RpcResponseObject {
  val header = 0x03
}
