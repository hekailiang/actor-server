package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseUploadCompleted(location: models.FileLocation) extends RpcResponseMessage {
  val header = ResponseUploadCompleted.header
}

object ResponseUploadCompleted extends RpcResponseMessageObject {
  val header = 0x17
}
