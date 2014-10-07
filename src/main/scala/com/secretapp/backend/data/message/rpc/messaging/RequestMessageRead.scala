package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1l)
case class RequestMessageRead(uid: Int, randomId: Long, accessHash: Long) extends RpcRequestMessage {
  val header = RequestMessageRead.requestType
}

object RequestMessageRead extends RpcRequestMessageObject {
  val requestType = 0x39
}
