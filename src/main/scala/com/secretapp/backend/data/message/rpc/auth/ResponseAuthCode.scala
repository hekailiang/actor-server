package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

case class ResponseAuthCode(smsHash : String, isRegistered : Boolean) extends RpcRequestMessage
object ResponseAuthCode extends RpcRequestMessageObject {
  val requestType = 0x2
}
