package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestRemoveContact(userId: Int, accessHash: Long) extends RpcRequestMessage {
  val header = RequestRemoveContact.header
}

object RequestRemoveContact extends RpcRequestMessageObject {
  val header = 0x59
}
