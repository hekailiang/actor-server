package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestDeleteChat(peer: struct.OutPeer) extends RpcRequestMessage {
  val header = RequestDeleteChat.header
}

object RequestDeleteChat extends RpcRequestMessageObject {
  val header = 0x64
}
