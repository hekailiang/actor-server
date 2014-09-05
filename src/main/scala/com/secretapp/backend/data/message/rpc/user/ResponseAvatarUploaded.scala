package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.Avatar

case class ResponseAvatarUploaded(avatar: Avatar) extends RpcResponseMessage

object ResponseAvatarUploaded extends RpcResponseMessageObject {
  val responseType = 0x20
}
