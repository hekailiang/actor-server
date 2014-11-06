package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

@SerialVersionUID(1L)
case class ResponseMessageSent(seq: Int, state: Option[UUID], date: Long) extends RpcResponseMessage {
  val header = ResponseMessageSent.header
}

object ResponseMessageSent extends RpcResponseMessageObject {
  val header = 0x73
}
