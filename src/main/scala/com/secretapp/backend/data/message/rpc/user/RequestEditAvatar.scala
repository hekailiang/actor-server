package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.struct.FileLocation
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestEditAvatar(fileLocation: FileLocation) extends RpcRequestMessage {
  val header = RequestEditAvatar.header
}

object RequestEditAvatar extends RpcRequestMessageObject {
  val header = 0x1F
}
