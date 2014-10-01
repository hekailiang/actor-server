package com.secretapp.backend.data.message.rpc

case class ResponseVoid() extends RpcResponseMessage {
  val header = ResponseVoid.responseType
}

object ResponseVoid extends RpcResponseMessageObject {
  val responseType = 0x32
}
