package com.secretapp.backend.data.message.rpc.contact

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class ResponseFindContacts(users: immutable.Seq[struct.User]) extends RpcResponseMessage {
  val header = ResponseFindContacts.header
}

object ResponseFindContacts extends RpcResponseMessageObject {
  val header = 0x71
}
