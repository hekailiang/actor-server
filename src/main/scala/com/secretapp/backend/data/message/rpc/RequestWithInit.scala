package com.secretapp.backend.data.message.rpc

// TODO: initConnection: InitConnection
case class RequestWithInit(initConnection: scodec.bits.BitVector, body: RpcRequestMessage) extends RpcRequest
object RequestWithInit extends RpcRequestObject {
  val rpcType = 0x2
}
