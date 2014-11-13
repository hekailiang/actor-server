package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestMessageDelete(peer: struct.OutPeer, randomId: Long) extends RequestWithRandomId {
  val header = RequestMessageDelete.header
}

object RequestMessageDelete extends RpcRequestMessageObject {
  val header = 0x62
}
