package com.secretapp.backend.data.message.rpc.contact

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import java.util.UUID

@SerialVersionUID(1L)
case class ResponseImportContacts(users: immutable.Seq[struct.User], seq: Int, state: Option[UUID]) extends RpcResponseMessage {
  val header = ResponseImportContacts.header
}

object ResponseImportContacts extends RpcResponseMessageObject {
  val header = 0x08
}
