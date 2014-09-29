package com.secretapp.backend.data.message.rpc

case class Ok(body: RpcResponseMessage) extends RpcResponse {
  override val rpcType = Ok.rpcType
}
object Ok extends RpcResponseObject {
  val rpcType = 0x1
}
