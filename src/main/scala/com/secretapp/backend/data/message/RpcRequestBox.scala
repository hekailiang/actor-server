package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcRequest

case class RpcRequestBox(body: RpcRequest) extends TransportMessage {
  override val header = RpcRequestBox.header
}
object RpcRequestBox extends TransportMessageMessageObject {
  override val header = 0x3
}
