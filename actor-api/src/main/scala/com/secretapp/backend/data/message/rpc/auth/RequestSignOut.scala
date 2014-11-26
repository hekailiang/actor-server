package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestSignOut() extends RpcRequestMessage {
  val header = RequestSignOut.header
}

object RequestSignOut extends RpcRequestMessageObject {
  val header = 0x54
}
