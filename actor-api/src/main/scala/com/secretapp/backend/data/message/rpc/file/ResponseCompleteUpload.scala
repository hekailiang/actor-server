package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseCompleteUpload(location: models.FileLocation) extends RpcResponseMessage {
  val header = ResponseCompleteUpload.header
}

object ResponseCompleteUpload extends RpcResponseMessageObject {
  val header = 0x17
}
