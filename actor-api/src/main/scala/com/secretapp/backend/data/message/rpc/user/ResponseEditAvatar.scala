package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.models
import java.util.UUID

@SerialVersionUID(1L)
case class ResponseEditAvatar(avatar: models.Avatar, seq: Int, state: Option[UUID]) extends RpcResponseMessage {
  val header = ResponseEditAvatar.header
}

object ResponseEditAvatar extends RpcResponseMessageObject {
  val header = 0x67
}
