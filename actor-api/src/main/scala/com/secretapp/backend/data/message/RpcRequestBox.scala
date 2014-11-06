package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcRequest

@SerialVersionUID(1L)
case class RpcRequestBox(body: RpcRequest) extends TransportMessage {
  val header = RpcRequestBox.header
}

object RpcRequestBox extends TransportMessageMessageObject {
  val header = 0x03
}
