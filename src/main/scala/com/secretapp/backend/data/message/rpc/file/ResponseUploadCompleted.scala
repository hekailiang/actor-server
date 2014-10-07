package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1l)
case class ResponseUploadCompleted(location: FileLocation) extends RpcResponseMessage {
  val header = ResponseUploadCompleted.responseType
}

object ResponseUploadCompleted extends RpcResponseMessageObject {
  val responseType = 0x17
}
