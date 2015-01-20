package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import com.eaio.uuid.UUID

@SerialVersionUID(1L)
case class RequestGetDifference(seq: Int, state: Option[UUID]) extends RpcRequestMessage {
  val header = RequestGetDifference.header
}

object RequestGetDifference extends RpcRequestMessageObject {
  val header = 0x0B
}
