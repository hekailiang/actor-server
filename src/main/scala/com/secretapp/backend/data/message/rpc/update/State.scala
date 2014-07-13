package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

case class State() extends RpcRequestMessage
object State extends RpcResponseMessageObject {
  val responseType = 0xa
}
