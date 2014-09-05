package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._

case class ResponseOnline() extends RpcResponseMessage

object ResponseOnline extends RpcResponseMessageObject {
  val responseType = 0x1E
}
