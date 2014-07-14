package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestGetDifference(seq : Int, state : BitVector) extends RpcRequestMessage
object RequestGetDifference extends RpcRequestMessageObject {
  val requestType = 0xb
}
