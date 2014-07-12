package com.secretapp.backend.data.message.rpc

case class Request(requestType : Int, body : RpcRequestMessage) extends RpcRequest
object Request {
  val header = 0x1
}
