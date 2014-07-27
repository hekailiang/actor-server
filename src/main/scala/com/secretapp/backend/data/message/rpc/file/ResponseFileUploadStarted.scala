package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class ResponseFileUploadStarted() extends RpcResponseMessage

object ResponseFileUploadStarted extends RpcResponseMessageObject {
  val responseType = 0x15
}
