package com.secretapp.backend.data.message.rpc.typing

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestTyping(uid: Int, accessHash: Long, typingType: Int) extends RpcRequestMessage {
  val header = RequestTyping.requestType
}

object RequestTyping extends RpcRequestMessageObject {
  val requestType = 0x1B
}
