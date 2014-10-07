package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

@SerialVersionUID(1l)
case class RequestUnregisterPush() extends RpcRequestMessage {
  val header = RequestUnregisterPush.requestType
}

object RequestUnregisterPush extends RpcRequestMessageObject {
  val requestType = 0x34
}
