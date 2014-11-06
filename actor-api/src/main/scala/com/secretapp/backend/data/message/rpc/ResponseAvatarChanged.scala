package com.secretapp.backend.data.message.rpc

import com.secretapp.backend.models

@SerialVersionUID(1L)
case class ResponseAvatarChanged(avatar: models.Avatar) extends RpcResponseMessage {
  val header = ResponseAvatarChanged.header
}

object ResponseAvatarChanged extends RpcResponseMessageObject {
  val header = 0x44
}
