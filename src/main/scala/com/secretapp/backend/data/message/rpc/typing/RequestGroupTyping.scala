package com.secretapp.backend.data.message.rpc.typing

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestGroupTyping(chatId: Int, accessHash: Long, typingType: Int) extends RpcRequestMessage

object RequestGroupTyping extends RpcRequestMessageObject {
  val requestType = 0x48
}
