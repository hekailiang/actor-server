package com.secretapp.backend.data.message.rpc.contact

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._

@SerialVersionUID(1L)
case class RequestImportContacts(contacts: immutable.Seq[ContactToImport]) extends RpcRequestMessage {
  val header = RequestImportContacts.requestType
}

object RequestImportContacts extends RpcRequestMessageObject {
  val requestType = 0x07
}
