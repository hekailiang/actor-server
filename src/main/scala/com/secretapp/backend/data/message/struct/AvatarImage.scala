package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import com.reactive.messenger.{ api => protobuf }
import com.secretapp.backend.data.message.rpc.file.FileLocation

case class AvatarImage(fileLocation: FileLocation, width: Int, height: Int) extends ProtobufMessage {
  def toProto = protobuf.AvatarImage(fileLocation.toProto, width, height)
}

object AvatarImage {
  def fromProto(avatarImage: protobuf.AvatarImage): AvatarImage = avatarImage match {
    case protobuf.AvatarImage(fileLocation, width, height) =>
      AvatarImage(FileLocation.fromProto(fileLocation), width, height)
  }
}
