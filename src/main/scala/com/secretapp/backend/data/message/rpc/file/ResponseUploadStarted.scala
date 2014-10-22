package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseUploadStarted(config: UploadConfig) extends RpcResponseMessage {
  val header = ResponseUploadStarted.header
}

object ResponseUploadStarted extends RpcResponseMessageObject {
  val header = 0x13
}
