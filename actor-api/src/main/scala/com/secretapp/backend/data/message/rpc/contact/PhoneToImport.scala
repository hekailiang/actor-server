package com.secretapp.backend.data.message.rpc.contact

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class PhoneToImport(phoneNumber: Long, contactName: Option[String]) extends ProtobufMessage
{
  def toProto = protobuf.PhoneToImport(phoneNumber, contactName)
}

object PhoneToImport {
  def fromProto(c: protobuf.PhoneToImport): PhoneToImport = c match {
    case protobuf.PhoneToImport(phoneNumber, contactName) => PhoneToImport(phoneNumber, contactName)
  }
}
