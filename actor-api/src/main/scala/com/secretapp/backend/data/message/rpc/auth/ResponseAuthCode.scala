package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseAuthCode(smsHash: String, isRegistered: Boolean) extends RpcResponseMessage {
  val header = ResponseAuthCode.header
}

object ResponseAuthCode extends RpcResponseMessageObject {
  val header = 0x02
}
