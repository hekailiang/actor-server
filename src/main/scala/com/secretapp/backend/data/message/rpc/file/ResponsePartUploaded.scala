package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class ResponsePartUploaded() extends RpcResponseMessage {
  override val header = ResponsePartUploaded.responseType
}

object ResponsePartUploaded extends RpcResponseMessageObject {
  override val responseType = 0x15
}
