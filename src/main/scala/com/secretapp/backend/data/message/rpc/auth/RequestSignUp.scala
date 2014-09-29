package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{RpcRequestMessage, RpcRequestMessageObject}
import scodec.bits.BitVector

case class RequestSignUp(phoneNumber: Long,
                         smsHash: String,
                         smsCode: String,
                         name: String,
                         publicKey: BitVector) extends RpcRequestMessage {
  override val header = RequestSignUp.requestType
}

object RequestSignUp extends RpcRequestMessageObject {
  override val requestType = 0x4
}
