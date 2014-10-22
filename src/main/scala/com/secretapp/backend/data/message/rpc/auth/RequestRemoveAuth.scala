package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestRemoveAuth(id: Int) extends RpcRequestMessage {
  val header = RequestRemoveAuth.requestType
}

object RequestRemoveAuth extends RpcRequestMessageObject {
  val requestType = 0x52
}
