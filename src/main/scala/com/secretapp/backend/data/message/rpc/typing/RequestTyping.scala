package com.secretapp.backend.data.message.rpc.typing

import com.secretapp.backend.data.message.rpc._

case class RequestTyping(uid: Int, accessHash: Long, typingType: Int) extends RpcRequestMessage

object RequestTyping extends RpcRequestMessageObject {
  val requestType = 0x1B
}
