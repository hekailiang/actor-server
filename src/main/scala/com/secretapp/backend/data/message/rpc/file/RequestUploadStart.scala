package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class RequestUploadStart() extends RpcRequestMessage

object RequestUploadStart extends RpcRequestMessageObject {
  val requestType = 0x12
}
