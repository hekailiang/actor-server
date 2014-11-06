package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1L)
case class ResponseVoid() extends RpcResponseMessage {
  val header = ResponseVoid.header
}

object ResponseVoid extends RpcResponseMessageObject {
  val header = 0x32
}
