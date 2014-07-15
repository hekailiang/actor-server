package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class ResponseSendMessage(mid: Int,
                               seq: Int,
                               state: BitVector) extends RpcResponseMessage
object ResponseSendMessage extends RpcResponseMessageObject {
  val responseType = 0xf
}
