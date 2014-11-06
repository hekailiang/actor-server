package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestRemoveAuth(id: Int) extends RpcRequestMessage {
  val header = RequestRemoveAuth.header
}

object RequestRemoveAuth extends RpcRequestMessageObject {
  val header = 0x52
}
