package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import com.eaio.uuid.UUID

@SerialVersionUID(1L)
case class ResponseSeq(seq: Int, state: Option[UUID]) extends RpcResponseMessage {
  val header = ResponseSeq.header
}

object ResponseSeq extends RpcResponseMessageObject {
  val header = 0x48
}
