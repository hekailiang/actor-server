package com.secretapp.backend.data.message.rpc

case class RequestWithInit(initConnection: InitConnection, body: RpcRequestMessage) extends RpcRequest {
  override val rpcType = RequestWithInit.rpcType
}

object RequestWithInit extends RpcRequestObject {
  override val rpcType = 0x2
}
