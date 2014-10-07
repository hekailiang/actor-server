package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import im.actor.messenger.{ api => protobuf }
import com.secretapp.backend.data.message.rpc.file.FileLocation

@SerialVersionUID(1l)
case class AvatarImage(fileLocation: FileLocation, width: Int, height: Int, fileSize: Int) extends ProtobufMessage {
  def toProto = protobuf.AvatarImage(fileLocation.toProto, width, height, fileSize)
}

object AvatarImage {
  def fromProto(avatarImage: protobuf.AvatarImage): AvatarImage = avatarImage match {
    case protobuf.AvatarImage(fileLocation, width, height, fileSize) =>
      AvatarImage(FileLocation.fromProto(fileLocation), width, height, fileSize)
  }
}
