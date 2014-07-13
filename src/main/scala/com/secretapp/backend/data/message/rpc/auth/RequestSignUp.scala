package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._

case class RequestSignUp(phoneNumber : Long,
                         smsHash : String,
                         smsCode : String,
                         firstName : String,
                         lastName : Option[String],
                         publicKey : List[Byte]) extends RpcRequestMessage
object RequestSignUp extends RpcRequestMessageObject {
  val requestType = 0x4
}
