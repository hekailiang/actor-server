package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcResponse

case class RpcResponseBox(messageId: Long, body: RpcResponse) extends TransportMessage {
  override val header = RpcResponseBox.header
}

object RpcResponseBox extends TransportMessageMessageObject {
  override val header = 0x4
}
