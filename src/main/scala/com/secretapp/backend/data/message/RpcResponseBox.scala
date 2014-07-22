package com.secretapp.backend.data.message

import com.secretapp.backend.data.message.rpc.RpcResponse

case class RpcResponseBox(messageId: Long, body: RpcResponse) extends TransportMessage

object RpcResponseBox extends TransportMessageMessageObject {
  val header = 0x4
}
