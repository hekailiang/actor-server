package com.secretapp.backend.data.message.rpc

case class ResponseVoid() extends RpcResponseMessage {
  override val header = ResponseVoid.responseType
}
object ResponseVoid extends RpcResponseMessageObject {
  override val responseType = 0x32
}
