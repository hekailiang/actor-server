package com.secretapp.backend.data.message.rpc.typing

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestGroupTyping(chatId: Int, accessHash: Long, typingType: Int) extends RpcRequestMessage {
  val header = RequestGroupTyping.header
}

object RequestGroupTyping extends RpcRequestMessageObject {
  val header = 0x48
}
