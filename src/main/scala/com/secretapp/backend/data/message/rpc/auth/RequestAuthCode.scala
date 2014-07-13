package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.RpcRequestMessage

case class RequestAuthCode(phoneNumber : Long, appId : Int, apiKey : String) extends RpcRequestMessage
object RequestAuthCode {
  val header = 0x1
}
