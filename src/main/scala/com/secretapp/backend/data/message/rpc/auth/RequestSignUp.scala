package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

case class RequestSignUp(phoneNumber: Long,
                         smsHash: String,
                         smsCode: String,
                         firstName: String,
                         lastName: Option[String],
                         publicKey: BitVector) extends RpcRequestMessage

object RequestSignUp extends RpcRequestMessageObject {
  val requestType = 0x4
}
