package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

case class RequestSignIn(phoneNumber : Long,
                         smsHash : String,
                         smsCode : String,
                         publicKey : List[Byte]) extends RpcRequestMessage
object RequestSignIn extends RpcRequestMessageObject {
  val requestType = 0x3
}
