package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class EmailToImport(email: String, contactName: Option[String]) extends ProtobufMessage
{
  def toProto = protobuf.EmailToImport(email, contactName)
}

object EmailToImport {
  def fromProto(c: protobuf.EmailToImport): EmailToImport = c match {
    case protobuf.EmailToImport(email, contactName) => EmailToImport(email, contactName)
  }
}
