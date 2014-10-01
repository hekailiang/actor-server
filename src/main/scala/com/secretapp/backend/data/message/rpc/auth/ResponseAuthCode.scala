package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

case class ResponseAuthCode(smsHash: String, isRegistered: Boolean) extends RpcResponseMessage {
  val header = ResponseAuthCode.responseType
}

object ResponseAuthCode extends RpcResponseMessageObject {
  val responseType = 0x02
}
