package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestGetContacts(contactsHash: String) extends RpcRequestMessage
{
  val header = RequestGetContacts.header
}

object RequestGetContacts extends RpcRequestMessageObject {
  val header = 0x57
}
