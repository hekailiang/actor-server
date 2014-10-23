package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestRemoveAllOtherAuths() extends RpcRequestMessage {
  val header = RequestRemoveAllOtherAuths.requestType
}

object RequestRemoveAllOtherAuths extends RpcRequestMessageObject {
  val requestType = 0x53
}
