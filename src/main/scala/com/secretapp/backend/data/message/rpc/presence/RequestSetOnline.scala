package com.secretapp.backend.data.message.rpc.presence

import com.secretapp.backend.data.message.rpc._

case class RequestSetOnline(isOnline: Boolean, timeout: Long) extends RpcRequestMessage

object RequestSetOnline extends RpcRequestMessageObject {
  val requestType = 0x1D
}
