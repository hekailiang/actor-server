package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class ResponseMessageRead(seq: Int) extends RpcResponseMessage

object ResponseMessageRead extends RpcResponseMessageObject {
  val responseType = 0x40
}
