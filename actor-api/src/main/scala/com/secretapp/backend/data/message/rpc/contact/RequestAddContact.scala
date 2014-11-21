package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestAddContact(userId: Int, accessHash: Long) extends RpcRequestMessage {
  val header = RequestAddContact.header
}

object RequestAddContact extends RpcRequestMessageObject {
  val header = 0x72
}
