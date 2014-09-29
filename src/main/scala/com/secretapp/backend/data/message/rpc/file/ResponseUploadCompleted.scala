package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class ResponseUploadCompleted(location: FileLocation) extends RpcResponseMessage {
  override val header = ResponseUploadCompleted.responseType
}

object ResponseUploadCompleted extends RpcResponseMessageObject {
  override val responseType = 0x17
}
