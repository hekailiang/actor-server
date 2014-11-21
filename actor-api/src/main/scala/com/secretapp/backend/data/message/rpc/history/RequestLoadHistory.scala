package com.secretapp.backend.data.message.rpc.history

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class RequestLoadHistory(outPeer: struct.OutPeer, startDate: Long, limit: Int) extends RpcRequestMessage {
  val header = RequestLoadHistory.header
}

object RequestLoadHistory extends RpcRequestMessageObject {
  val header = 0x76
}
