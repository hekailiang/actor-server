package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestSearchContacts(request: String) extends RpcRequestMessage {
  val header = RequestSearchContacts.header
}

object RequestSearchContacts extends RpcRequestMessageObject {
  val header = 0x70
}
