package com.secretapp.backend.data.message.rpc.file

import com.secretapp.backend.data.message.rpc._

case class ResponsePartUploaded() extends RpcResponseMessage

object ResponsePartUploaded extends RpcResponseMessageObject {
  val responseType = 0x15
}
