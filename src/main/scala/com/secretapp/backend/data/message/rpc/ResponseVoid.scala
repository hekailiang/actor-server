package com.secretapp.backend.data.message.rpc

@SerialVersionUID(1l)
case class ResponseVoid() extends RpcResponseMessage {
  val header = ResponseVoid.responseType
}

object ResponseVoid extends RpcResponseMessageObject {
  val responseType = 0x32
}
