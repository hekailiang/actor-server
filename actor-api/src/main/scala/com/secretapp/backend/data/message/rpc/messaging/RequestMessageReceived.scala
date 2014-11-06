package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestMessageReceived(peer: struct.OutPeer, date: Long) extends RpcRequestMessage {
  val header = RequestMessageReceived.header
}

object RequestMessageReceived extends RpcRequestMessageObject {
  val header = 0x37
}
