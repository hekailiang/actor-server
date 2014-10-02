package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

case class RequestMessageReceived(uid: Int, randomId: Long, accessHash: Long) extends RpcRequestMessage {
  override val header = RequestMessageReceived.requestType
}

object RequestMessageReceived extends RpcRequestMessageObject {
  override val requestType = 0x37
}
