package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class ResponseSendAuthCode(smsHash: String, isRegistered: Boolean) extends RpcResponseMessage {
  val header = ResponseSendAuthCode.header
}

object ResponseSendAuthCode extends RpcResponseMessageObject {
  val header = 0x02
}
