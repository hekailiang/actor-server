package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.Avatar

@SerialVersionUID(1L)
case class ResponseAvatarChanged(avatar: Avatar) extends RpcResponseMessage {
  val header = ResponseAvatarChanged.header
}

object ResponseAvatarChanged extends RpcResponseMessageObject {
  val header = 0x44
}
