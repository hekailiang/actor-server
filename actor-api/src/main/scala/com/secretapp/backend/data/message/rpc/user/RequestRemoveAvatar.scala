package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestRemoveAvatar() extends RpcRequestMessage {
  val header = RequestRemoveAvatar.header
}

object RequestRemoveAvatar extends RpcRequestMessageObject {
  val header = 0x5B
}
