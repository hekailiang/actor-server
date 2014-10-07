package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1l)
case class RequestGetState() extends RpcRequestMessage {
  val header = RequestGetState.requestType
}

object RequestGetState extends RpcRequestMessageObject {
  val requestType = 0x09
}
