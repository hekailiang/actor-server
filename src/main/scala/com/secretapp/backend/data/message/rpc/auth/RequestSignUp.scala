package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{RpcRequestMessage, RpcRequestMessageObject}
import scodec.bits.BitVector

@SerialVersionUID(1l)
case class RequestSignUp(phoneNumber: Long,
                         smsHash: String,
                         smsCode: String,
                         name: String,
                         publicKey: BitVector) extends RpcRequestMessage {
  val header = RequestSignUp.requestType
}

object RequestSignUp extends RpcRequestMessageObject {
  val requestType = 0x04
}
