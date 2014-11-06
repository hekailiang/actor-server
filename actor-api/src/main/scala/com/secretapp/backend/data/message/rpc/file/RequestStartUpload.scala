package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestStartUpload() extends RpcRequestMessage {
  val header = RequestStartUpload.header
}

object RequestStartUpload extends RpcRequestMessageObject {
  val header = 0x12
}
