package com.secretapp.backend.data.message.rpc

import com.secretapp.backend.models
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class ResponseAvatarChanged(avatar: models.Avatar, seq: Int, state: Option[UUID]) extends RpcResponseMessage {
  val header = ResponseAvatarChanged.header
}

object ResponseAvatarChanged extends RpcResponseMessageObject {
  val header = 0x44
}
