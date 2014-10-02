package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.Avatar

case class ResponseAvatarChanged(avatar: Avatar) extends RpcResponseMessage {
  override val header = ResponseAvatarChanged.responseType
}

object ResponseAvatarChanged extends RpcResponseMessageObject {
  override val responseType = 0x44
}
