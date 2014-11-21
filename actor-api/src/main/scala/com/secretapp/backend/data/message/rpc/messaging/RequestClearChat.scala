package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestClearChat(outPeer: struct.OutPeer) extends RpcRequestMessage {
  val header = RequestClearChat.header
}

object RequestClearChat extends RpcRequestMessageObject {
  val header = 0x63
}
