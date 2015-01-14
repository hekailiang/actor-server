package com.secretapp.backend.data.message.rpc.messaging

import com.eaio.uuid.UUID
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseSeqDate(seq: Int, state: Option[UUID], date: Long) extends RpcResponseMessage {
  val header = ResponseSeqDate.header
}

object ResponseSeqDate extends RpcResponseMessageObject {
  val header = 0x66
}
