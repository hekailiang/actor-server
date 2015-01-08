package com.secretapp.backend.data.message.rpc.user

import com.secretapp.backend.data.message.rpc.{RpcRequestMessageObject, RpcRequestMessage}

@SerialVersionUID(1L)
case class RequestChangePhoneTitle(phoneId: Int, title: String) extends RpcRequestMessage {
  val header = RequestChangePhoneTitle.header
}

object RequestChangePhoneTitle extends RpcRequestMessageObject {
  val header = 0x7C
}
