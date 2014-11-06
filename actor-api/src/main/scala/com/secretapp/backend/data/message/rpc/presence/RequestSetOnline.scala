package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestSetOnline(isOnline: Boolean, timeout: Long) extends RpcRequestMessage {
  val header = RequestSetOnline.header
}

object RequestSetOnline extends RpcRequestMessageObject {
  val header = 0x1D
}
