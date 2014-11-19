package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestMessageRead(outPeer: struct.OutPeer, date: Long) extends RpcRequestMessage {
  val header = RequestMessageRead.header
}

object RequestMessageRead extends RpcRequestMessageObject {
  val header = 0x39
}
