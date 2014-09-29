package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._

case class ResponseOnline() extends RpcResponseMessage {
  override val header = ResponseOnline.responseType
}

object ResponseOnline extends RpcResponseMessageObject {
  val responseType = 0x1E
}
