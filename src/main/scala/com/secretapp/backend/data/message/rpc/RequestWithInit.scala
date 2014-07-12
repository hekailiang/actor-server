package com.secretapp.backend.data.message.rpc

case class RequestWithInit(initConnection : InitConnection, requestType : Int, body : RpcRequestMessage)
object RequestWithInit {
  val header = 0x2
}
