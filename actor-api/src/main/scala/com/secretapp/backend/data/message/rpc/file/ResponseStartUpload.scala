package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseStartUpload(config: UploadConfig) extends RpcResponseMessage {
  val header = ResponseStartUpload.header
}

object ResponseStartUpload extends RpcResponseMessageObject {
  val header = 0x13
}
