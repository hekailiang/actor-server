package com.secretapp.backend.data.message.rpc

case class FloodWait(delay: Int) extends RpcResponse
object FloodWait extends RpcResponseObject {
  val rpcType = 0x3
}
