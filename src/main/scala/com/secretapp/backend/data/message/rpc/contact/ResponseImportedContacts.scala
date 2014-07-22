package com.secretapp.backend.data.message.rpc.contact

import scala.collection.immutable
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.struct

case class ResponseImportedContacts(users: immutable.Seq[struct.User],
                                    contacts: immutable.Seq[ImportedContact]) extends RpcResponseMessage

object ResponseImportedContacts extends RpcResponseMessageObject {
  val responseType = 0x8
}
