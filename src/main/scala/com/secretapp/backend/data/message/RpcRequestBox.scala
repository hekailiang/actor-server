package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcRequestMessage

case class RpcRequestBox(body : RpcRequestMessage) extends TransportMessage
object RpcRequestBox {
  val header = 0x3
}
