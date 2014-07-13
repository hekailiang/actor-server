package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc.RpcRequestMessage

case class State() extends RpcRequestMessage
object State {
  val header = 0xa
}
