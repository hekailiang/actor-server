package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }

@SerialVersionUID(1L)
case class RequestRemoveAllOtherAuths() extends RpcRequestMessage {
  val header = RequestRemoveAllOtherAuths.header
}

object RequestRemoveAllOtherAuths extends RpcRequestMessageObject {
  val header = 0x53
}
