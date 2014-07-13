package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc.RpcRequestMessage

case class RequestGetDifference(seq : Int, state : List[Byte]) extends RpcRequestMessage
object RequestGetDifference {
  val header = 0xb
}
