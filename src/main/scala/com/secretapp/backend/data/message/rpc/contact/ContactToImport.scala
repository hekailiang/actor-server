package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import com.reactive.messenger.{ api => protobuf }

case class ContactToImport(clientPhoneId: Long, phoneNumber: Long) extends ProtobufMessage
{
  def toProto = protobuf.ContactToImport(clientPhoneId, phoneNumber)
}

object ContactToImport {
  def fromProto(c: protobuf.ContactToImport): ContactToImport = c match {
    case protobuf.ContactToImport(clientPhoneId, phoneNumber) => ContactToImport(clientPhoneId, phoneNumber)
  }
}
