package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestEditAvatar(fileLocation: models.FileLocation) extends RpcRequestMessage {
  val header = RequestEditAvatar.header
}

object RequestEditAvatar extends RpcRequestMessageObject {
  val header = 0x1F
}
