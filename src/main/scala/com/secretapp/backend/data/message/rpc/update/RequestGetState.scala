package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

case class RequestGetState() extends RpcRequestMessage {
  override val header = RequestGetState.requestType
}
object RequestGetState extends RpcRequestMessageObject {
  override val requestType = 0x9
}
