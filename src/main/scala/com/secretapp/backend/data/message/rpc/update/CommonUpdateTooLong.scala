package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc.RpcRequestMessage

case class CommonUpdateTooLong() extends RpcRequestMessage
object CommonUpdateTooLong {
  val header = 0x19
}
