package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

@SerialVersionUID(1l)
case class RequestGetDifference(seq: Int, state: Option[UUID]) extends RpcRequestMessage {
  val header = RequestGetDifference.requestType
}

object RequestGetDifference extends RpcRequestMessageObject {
  val requestType = 0x0B
}
