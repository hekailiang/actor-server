package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

@SerialVersionUID(1L)
case class ResponseSeqDate(seq: Int, state: Option[UUID], date: Long) extends RpcResponseMessage {
  val header = ResponseSeqDate.header
}

object ResponseSeqDate extends RpcResponseMessageObject {
  val header = 0x66
}
