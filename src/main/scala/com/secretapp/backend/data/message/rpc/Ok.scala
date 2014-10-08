package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class Ok(body: RpcResponseMessage) extends RpcResponse {
  val rpcType = Ok.rpcType
}

object Ok extends RpcResponseObject {
  val rpcType = 0x01
}
