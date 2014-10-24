package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class Ok(body: RpcResponseMessage) extends RpcResponse {
  val header = Ok.header
}

object Ok extends RpcResponseObject {
  val header = 0x01
}
