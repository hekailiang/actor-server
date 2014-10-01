package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.file.FileLocation

case class RequestEditAvatar(fileLocation: FileLocation) extends RpcRequestMessage {
  val header = RequestEditAvatar.requestType
}

object RequestEditAvatar extends RpcRequestMessageObject {
  val requestType = 0x1F
}
