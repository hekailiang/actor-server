package com.secretapp.backend.data.message.rpc.contact

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

@SerialVersionUID(1L)
case class ResponseGetContacts(users: immutable.Seq[struct.User], isNotChanged: Boolean) extends RpcResponseMessage {
  val header = ResponseGetContacts.header
}

object ResponseGetContacts extends RpcResponseMessageObject {
  val header = 0x58
}
