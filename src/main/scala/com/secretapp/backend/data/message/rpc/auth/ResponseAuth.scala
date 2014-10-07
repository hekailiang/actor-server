package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1l)
case class ResponseAuth(publicKeyHash: Long, user: struct.User) extends RpcResponseMessage {
  val header = ResponseAuth.responseType
}

object ResponseAuth extends RpcResponseMessageObject {
  val responseType = 0x05
}
