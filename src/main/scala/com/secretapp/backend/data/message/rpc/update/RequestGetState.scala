package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc.RpcRequestMessage

case class RequestGetState() extends RpcRequestMessage
object RequestGetState {
  val header = 0x9
}
