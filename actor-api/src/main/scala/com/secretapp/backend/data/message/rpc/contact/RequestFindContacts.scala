package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestFindContacts(request: String) extends RpcRequestMessage {
  val header = RequestFindContacts.header
}

object RequestFindContacts extends RpcRequestMessageObject {
  val header = 0x70
}
