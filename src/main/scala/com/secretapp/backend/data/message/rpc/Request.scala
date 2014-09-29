package com.secretapp.backend.data.message.rpc

case class Request(body: RpcRequestMessage) extends RpcRequest {
  override val rpcType = Request.rpcType
}
object Request extends RpcRequestObject {
  override val rpcType = 0x1
}
