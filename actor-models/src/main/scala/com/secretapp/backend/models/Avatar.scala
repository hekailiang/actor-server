package com.secretapp.backend.models

@SerialVersionUID(1L)
case class Avatar(
  smallImage: Option[AvatarImage],
  largeImage: Option[AvatarImage],
  fullImage: Option[AvatarImage]
)
