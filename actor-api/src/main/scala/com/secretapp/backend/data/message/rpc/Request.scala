package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class Request(body: RpcRequestMessage) extends RpcRequest {
  val header = Request.header
}

object Request extends RpcRequestObject {
  val header = 0x01
}
