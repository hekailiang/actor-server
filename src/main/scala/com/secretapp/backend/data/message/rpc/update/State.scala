package com.secretapp.backend.data.message.rpc.update

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class State(seq: Int, state: BitVector) extends RpcResponseMessage
object State extends RpcResponseMessageObject {
  val responseType = 0xa
}
