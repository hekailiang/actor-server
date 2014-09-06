package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct.{Avatar => AvatarStruct}

case class Avatar(smallImage: Option[AvatarImage], largeImage: Option[AvatarImage], fullImage: Option[AvatarImage]) {
  lazy val toStruct = AvatarStruct(smallImage.map(_.toStruct), largeImage.map(_.toStruct), fullImage.map(_.toStruct))
}
