package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestMessageRead(uid: Int, randomId: Long, accessHash: Long) extends RpcRequestMessage {
  val header = RequestMessageRead.header
}

object RequestMessageRead extends RpcRequestMessageObject {
  val header = 0x39
}
