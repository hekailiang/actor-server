package com.secretapp.backend.data.message.rpc.file

import com.google.protobuf.{ ByteString => ProtoByteString }
import com.secretapp.backend.data.message.ProtobufMessage
import com.getsecretapp.{ proto => protobuf }

case class UploadConfig(serverData: ProtoByteString) extends ProtobufMessage
{
  def toProto = protobuf.UploadConfig(serverData)
}

object UploadConfig {
  def fromProto(r: protobuf.UploadConfig): UploadConfig = r match {
    case protobuf.UploadConfig(serverData) => UploadConfig(serverData)
  }
}
