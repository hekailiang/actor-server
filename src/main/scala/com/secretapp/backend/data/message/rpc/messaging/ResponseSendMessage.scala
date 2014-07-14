package com.secretapp.backend.data.message.rpc.messaging

import com.secretapp.backend.data.message.rpc._

case class ResponseSendMessage(mid : Int,
                               seq : Int,
                               state : List[Byte]) extends RpcResponseMessage
object ResponseSendMessage extends RpcResponseMessageObject {
  val responseType = 0xf
}
