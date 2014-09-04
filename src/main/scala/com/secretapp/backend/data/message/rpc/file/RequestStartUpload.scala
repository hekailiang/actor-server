package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class RequestStartUpload() extends RpcRequestMessage

object RequestStartUpload extends RpcRequestMessageObject {
  val requestType = 0x12
}
