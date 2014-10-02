package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

case class RequestEditName(name: String) extends RpcRequestMessage

object RequestEditName extends RpcRequestMessageObject {
  val requestType = 0x35
}
