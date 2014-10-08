package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import java.util.UUID
import scodec.bits._

@SerialVersionUID(1L)
case class ResponseCreateChat(
  chatId: Int, accessHash: Long, seq: Int, state: Option[UUID]
) extends RpcResponseMessage {
  val header = ResponseCreateChat.responseType
}

object ResponseCreateChat extends RpcResponseMessageObject {
  val responseType = 0x42
}
