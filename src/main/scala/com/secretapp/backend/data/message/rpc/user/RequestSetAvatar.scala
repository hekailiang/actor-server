package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.file.FileLocation

case class RequestSetAvatar(fileLocation: FileLocation) extends RpcRequestMessage

object RequestSetAvatar extends RpcRequestMessageObject {
  val requestType = 0x1F
}
