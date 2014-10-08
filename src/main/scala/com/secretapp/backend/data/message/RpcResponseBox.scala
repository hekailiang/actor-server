package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcResponse

@SerialVersionUID(1L)
case class RpcResponseBox(messageId: Long, body: RpcResponse) extends TransportMessage {
  val header = RpcResponseBox.header
}

object RpcResponseBox extends TransportMessageMessageObject {
  val header = 0x04
}
