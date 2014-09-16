package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcResponseMessageObject, RpcResponseMessage}

case class RequestUnregisterPush() extends RpcResponseMessage
object RequestUnregisterPush extends RpcResponseMessageObject {
  override val responseType = 0x34
}
