package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

case class RequestGetState() extends RpcRequestMessage
object RequestGetState extends RpcRequestMessageObject {
  val requestType = 0x09
}
