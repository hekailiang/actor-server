package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID

case class ResponseSendMessage(mid: Int,
                               seq: Int,
                               state: UUID) extends RpcResponseMessage {
  override val header = ResponseSendMessage.responseType
}
object ResponseSendMessage extends RpcResponseMessageObject {
  override val responseType = 0xf
}
