package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcRequest

case class RpcRequestBox(body : RpcRequest) extends TransportMessage
object RpcRequestBox extends TransportMessageMessageObject {
  val header = 0x3
}
