package com.secretapp.backend.data.message.rpc.typing

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestTyping(uid: Int, accessHash: Long, typingType: Int) extends RpcRequestMessage {
  val header = RequestTyping.header
}

object RequestTyping extends RpcRequestMessageObject {
  val header = 0x1B
}
