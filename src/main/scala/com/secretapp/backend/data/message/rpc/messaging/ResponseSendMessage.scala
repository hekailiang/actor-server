package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class ResponseSendMessage(mid: Int,
                               seq: Int,
                               state: UUID) extends RpcResponseMessage
object ResponseSendMessage extends RpcResponseMessageObject {
  val responseType = 0xf
}
