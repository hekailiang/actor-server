package com.secretapp.backend.data.message

import com.secretapp.backend.proto
import com.secretapp.backend.models
import im.actor.messenger.{ api => protobuf }

package object struct {

  implicit object FileLocationFormats extends proto.Formats[models.FileLocation, protobuf.FileLocation] {
    def toProto(a: models.FileLocation): protobuf.FileLocation =
      protobuf.FileLocation(a.fileId, a.accessHash)

    def fromProto(b: protobuf.FileLocation): models.FileLocation =
      models.FileLocation(b.fileId.toInt, b.accessHash)
  }

  implicit object AvatarImageFormats extends proto.Formats[models.AvatarImage, protobuf.AvatarImage] {
    override def fromProto(b: protobuf.AvatarImage): models.AvatarImage =
      models.AvatarImage(proto.fromProto[models.FileLocation, protobuf.FileLocation](b.fileLocation), b.width, b.height, b.fileSize)

    override def toProto(a: models.AvatarImage): protobuf.AvatarImage =
      protobuf.AvatarImage(proto.toProto(a.fileLocation), a.width, a.height, a.fileSize)
  }

  implicit object AvatarFormats extends proto.Formats[models.Avatar, protobuf.Avatar] {
    override def fromProto(b: protobuf.Avatar): models.Avatar =
      models.Avatar(
        b.smallImage map proto.fromProto[models.AvatarImage, protobuf.AvatarImage],
        b.largeImage map proto.fromProto[models.AvatarImage, protobuf.AvatarImage],
        b.fullImage map proto.fromProto[models.AvatarImage, protobuf.AvatarImage]
      )

    override def toProto(a: models.Avatar): protobuf.Avatar =
      protobuf.Avatar(
        a.smallImage map proto.toProto[models.AvatarImage, protobuf.AvatarImage],
        a.largeImage map proto.toProto[models.AvatarImage, protobuf.AvatarImage],
        a.fullImage map proto.toProto[models.AvatarImage, protobuf.AvatarImage]
      )
  }

}
