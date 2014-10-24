package com.secretapp.backend.data.message.rpc.push

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

case class RequestRegisterApplePush(apnsKey: Int, token: String) extends RpcRequestMessage {
  val header = RequestRegisterApplePush.header
}

object RequestRegisterApplePush extends RpcRequestMessageObject {
  override val header = 0x4C
}
