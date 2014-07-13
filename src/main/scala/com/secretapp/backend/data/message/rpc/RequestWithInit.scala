package com.secretapp.backend.data.message.rpc

case class RequestWithInit(initConnection : InitConnection, requestType : Int, body : RpcRequestMessage) extends RpcRequest
object RequestWithInit extends RpcRequestObject {
  val rpcType = 0x2
}
