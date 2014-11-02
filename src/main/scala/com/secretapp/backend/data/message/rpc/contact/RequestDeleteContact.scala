package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestDeleteContact(uid: Int, accessHash: Long) extends RpcRequestMessage {
  val header = RequestDeleteContact.header
}

object RequestDeleteContact extends RpcRequestMessageObject {
  val header = 0x59
}
