package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestGetFile(fileLocation: models.FileLocation, offset: Int, limit: Int) extends RpcRequestMessage {
  val header = RequestGetFile.header
}

object RequestGetFile extends RpcRequestMessageObject {
  val header = 0x10
}
