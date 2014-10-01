package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.Avatar

case class ResponseAvatarChanged(avatar: Avatar) extends RpcResponseMessage

object ResponseAvatarChanged extends RpcResponseMessageObject {
  val responseType = 0x44
}
