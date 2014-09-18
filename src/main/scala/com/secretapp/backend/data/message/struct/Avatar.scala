package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import com.reactive.messenger.{ api => protobuf }

case class Avatar(
  smallImage: Option[AvatarImage],
  largeImage: Option[AvatarImage],
  fullImage: Option[AvatarImage]) extends ProtobufMessage {
  def toProto = protobuf.Avatar(
    smallImage map (_.toProto),
    largeImage map (_.toProto),
    fullImage map (_.toProto))
}

object Avatar {
  def fromProto(avatar: protobuf.Avatar): Avatar = avatar match {
    case protobuf.Avatar(smallImage, largeImage, fullImage) =>
      Avatar(
        smallImage map AvatarImage.fromProto,
        largeImage map AvatarImage.fromProto,
        fullImage map AvatarImage.fromProto
      )
  }
}
