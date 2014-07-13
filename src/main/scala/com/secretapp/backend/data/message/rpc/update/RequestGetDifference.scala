package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._

case class RequestGetDifference(seq : Int, state : List[Byte]) extends RpcRequestMessage
object RequestGetDifference extends RpcResponseMessageObject {
  val responseType = 0xb
}
