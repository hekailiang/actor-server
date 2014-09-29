package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

case class RequestUnregisterPush() extends RpcRequestMessage {
  override val header = RequestUnregisterPush.requestType
}
object RequestUnregisterPush extends RpcRequestMessageObject {
  override val requestType = 0x34
}
