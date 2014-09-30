package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

case class ImportedContact(clientPhoneId: Long, userId: Int) extends ProtobufMessage
{
  def toProto = protobuf.ImportedContact(clientPhoneId, userId)
}

object ImportedContact {
  def fromProto(c: protobuf.ImportedContact): ImportedContact = c match {
    case protobuf.ImportedContact(clientPhoneId, userId) => ImportedContact(clientPhoneId, userId)
  }
}
