package com.secretapp.backend.data.message.rpc.auth

import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

case class ResponseAuth(publicKeyHash: Long, user: struct.User) extends RpcResponseMessage
object ResponseAuth extends RpcResponseMessageObject {
  val responseType = 0x5
}
