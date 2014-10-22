package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{RpcRequestMessage, RpcRequestMessageObject}
import scodec.bits.BitVector

@SerialVersionUID(1L)
case class RequestSignUp(phoneNumber: Long,
                         smsHash: String,
                         smsCode: String,
                         name: String,
                         publicKey: BitVector) extends RpcRequestMessage {
  val header = RequestSignUp.header
}

object RequestSignUp extends RpcRequestMessageObject {
  val header = 0x04
}
