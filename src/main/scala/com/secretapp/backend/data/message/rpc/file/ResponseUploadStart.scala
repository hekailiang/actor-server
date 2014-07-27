package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class ResponseUploadStart(config: UploadConfig) extends RpcResponseMessage

object ResponseUploadStart extends RpcResponseMessageObject {
  val responseType = 0x13
}
