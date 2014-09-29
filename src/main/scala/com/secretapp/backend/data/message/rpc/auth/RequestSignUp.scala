package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.RpcRequestMessageObject
import scodec.bits.BitVector

case class RequestSignUp(phoneNumber: Long,
                         smsHash: String,
                         smsCode: String,
                         name: String,
                         publicKey: BitVector) extends RequestSign {
  override val header = RequestSignUp.requestType
}

object RequestSignUp extends RpcRequestMessageObject {
  override val requestType = 0x4
}
