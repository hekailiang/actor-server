package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestLoadDialogs(endDate: Long, limit: Int) extends RpcRequestMessage {
  val header = RequestLoadDialogs.header
}

object RequestLoadDialogs extends RpcRequestMessageObject {
  val header = 0x68
}
