package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

@SerialVersionUID(1L)
case class RequestUnregisterPush() extends RpcRequestMessage {
  val header = RequestUnregisterPush.header
}

object RequestUnregisterPush extends RpcRequestMessageObject {
  val header = 0x34
}
