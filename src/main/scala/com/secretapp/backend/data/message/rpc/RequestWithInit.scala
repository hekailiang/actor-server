package com.secretapp.backend.data.message.rpc

case class RequestWithInit(initConnection : InitConnection, requestType : Int, body : RpcRequestMessage) extends RpcRequest
object RequestWithInit {
  val header = 0x2
}
