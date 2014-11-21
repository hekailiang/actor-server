package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestSendMessage(outPeer: struct.OutPeer, randomId: Long, message: MessageContent) extends RequestWithRandomId {
  val header = RequestSendMessage.header
}

object RequestSendMessage extends RpcRequestMessageObject {
  val header = 0x5C
}
