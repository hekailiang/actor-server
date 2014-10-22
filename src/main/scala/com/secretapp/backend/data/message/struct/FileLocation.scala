package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }

@SerialVersionUID(1L)
case class FileLocation(fileId: Long, accessHash: Long) extends ProtobufMessage
{
  def toProto = protobuf.FileLocation(fileId, accessHash)
}

object FileLocation {
  def fromProto(r: protobuf.FileLocation): FileLocation = r match {
    case protobuf.FileLocation(fileId, accessHash) => FileLocation(fileId.toInt, accessHash)
  }
}
