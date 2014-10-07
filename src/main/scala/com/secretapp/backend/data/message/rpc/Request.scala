package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1l)
case class Request(body: RpcRequestMessage) extends RpcRequest {
  val rpcType = Request.rpcType
}

object Request extends RpcRequestObject {
  val rpcType = 0x01
}
