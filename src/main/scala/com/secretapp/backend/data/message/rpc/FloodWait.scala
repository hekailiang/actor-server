package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class FloodWait(delay: Int) extends RpcResponse {
  val rpcType = FloodWait.rpcType
}

object FloodWait extends RpcResponseObject {
  val rpcType = 0x03
}
