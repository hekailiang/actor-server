package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

case class CommonUpdateTooLong() extends RpcRequestMessage
object CommonUpdateTooLong extends RpcResponseMessageObject {
  val responseType = 0x19
}
