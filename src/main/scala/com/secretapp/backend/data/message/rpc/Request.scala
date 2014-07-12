package com.secretapp.backend.data.message.rpc

case class Request(requestType : Int, body : RpcRequestMessage) extends RpcMessage
object Request {
  val header = 0x1
}
