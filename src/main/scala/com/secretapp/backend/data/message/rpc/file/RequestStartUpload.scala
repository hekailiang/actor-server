package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestStartUpload() extends RpcRequestMessage {
  val header = RequestStartUpload.requestType
}

object RequestStartUpload extends RpcRequestMessageObject {
  val requestType = 0x12
}
