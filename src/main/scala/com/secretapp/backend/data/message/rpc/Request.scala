package com.secretapp.backend.data.message.rpc

case class Request(body: RpcRequestMessage) extends RpcRequest
object Request extends RpcRequestObject {
  val rpcType = 0x1
}
