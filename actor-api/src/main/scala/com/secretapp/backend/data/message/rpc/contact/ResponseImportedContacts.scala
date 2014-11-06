package com.secretapp.backend.data.message.rpc.contact

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct
import scodec.bits._

@SerialVersionUID(1L)
case class ResponseImportedContacts(users: immutable.Seq[struct.User], seq: Int, state: BitVector) extends RpcResponseMessage {
  val header = ResponseImportedContacts.header
}

object ResponseImportedContacts extends RpcResponseMessageObject {
  val header = 0x08
}
