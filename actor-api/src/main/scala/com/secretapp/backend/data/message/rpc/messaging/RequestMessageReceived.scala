package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestMessageReceived(uid: Int, randomId: Long, accessHash: Long) extends RpcRequestMessage {
  val header = RequestMessageReceived.header
}

object RequestMessageReceived extends RpcRequestMessageObject {
  val header = 0x37
}
