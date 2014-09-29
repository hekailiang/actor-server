package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct.Avatar

case class ResponseAvatarUploaded(avatar: Avatar) extends RpcResponseMessage {
  override val header: Int = ResponseAvatarUploaded.responseType
}

object ResponseAvatarUploaded extends RpcResponseMessageObject {
  override val responseType = 0x20
}
