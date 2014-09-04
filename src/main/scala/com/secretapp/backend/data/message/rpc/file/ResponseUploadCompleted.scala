package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class ResponseUploadCompleted(location: FileLocation) extends RpcResponseMessage

object ResponseUploadCompleted extends RpcResponseMessageObject {
  val responseType = 0x17
}
