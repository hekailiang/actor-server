package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class ResponseAuth(publicKeyHash: Long, user: struct.User, config: struct.Config) extends RpcResponseMessage {
  val header = ResponseAuth.header
}

object ResponseAuth extends RpcResponseMessageObject {
  val header = 0x05
}
