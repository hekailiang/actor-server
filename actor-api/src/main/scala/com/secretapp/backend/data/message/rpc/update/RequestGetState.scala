package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestGetState() extends RpcRequestMessage {
  val header = RequestGetState.header
}

object RequestGetState extends RpcRequestMessageObject {
  val header = 0x09
}
