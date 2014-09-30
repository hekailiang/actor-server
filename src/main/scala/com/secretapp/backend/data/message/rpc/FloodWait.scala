package com.secretapp.backend.data.message.rpc

case class FloodWait(delay: Int) extends RpcResponse {
  override val rpcType = FloodWait.rpcType
}
object FloodWait extends RpcResponseObject {
  override val rpcType = 0x3
}
