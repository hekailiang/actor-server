package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class ResponseMessageReceived(seq: Int) extends RpcResponseMessage

object ResponseMessageReceived extends RpcResponseMessageObject {
  val responseType = 0x38
}
