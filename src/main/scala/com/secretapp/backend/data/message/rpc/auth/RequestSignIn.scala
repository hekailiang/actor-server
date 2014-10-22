package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcRequestMessageObject }
import scodec.bits.BitVector

@SerialVersionUID(2L)
case class RequestSignIn(
  phoneNumber: Long,
  smsHash: String,
  smsCode: String,
  publicKey: BitVector,
  deviceHash: BitVector,
  deviceTitle: String,
  appId: Int,
  appKey: String
) extends RpcRequestMessage {
  val header = RequestSignIn.requestType
}

object RequestSignIn extends RpcRequestMessageObject {
  val requestType = 0x03
}
