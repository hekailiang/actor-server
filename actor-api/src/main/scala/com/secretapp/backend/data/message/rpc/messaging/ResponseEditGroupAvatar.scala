package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.models
import java.util.UUID

@SerialVersionUID(1L)
case class ResponseEditGroupAvatar(avatar: models.Avatar, seq: Int, state: Option[UUID], date: Long) extends RpcResponseMessage {
  val header = ResponseEditGroupAvatar.header
}

object ResponseEditGroupAvatar extends RpcResponseMessageObject {
  val header = 0x73
}
