package com.secretapp.backend.data.message.rpc.typing

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestTyping(peer: struct.OutPeer, typingType: Int) extends RpcRequestMessage {
  val header = RequestTyping.header
}

object RequestTyping extends RpcRequestMessageObject {
  val header = 0x1B
}
