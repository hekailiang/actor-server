package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class RequestStartUpload() extends RpcRequestMessage {
  override val header = RequestStartUpload.requestType
}

object RequestStartUpload extends RpcRequestMessageObject {
  override val requestType = 0x12
}
