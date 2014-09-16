package com.secretapp.backend.data.message.rpc

case class ResponseVoid() extends RpcResponseMessage
object ResponseVoid extends RpcResponseMessageObject {
  val responseType = 0x32
}
