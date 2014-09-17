package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import scodec.bits.BitVector

trait RequestSign extends RpcRequestMessage {
  val phoneNumber: Long
  val smsHash: String
  val smsCode: String
  val publicKey: BitVector
}
