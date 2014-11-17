package models

import models.CommonJsonFormats._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.secretapp.backend.models.AvatarImage

case class Avatar(
  smallImage: Option[AvatarImage],
  largeImage: Option[AvatarImage],
  fullImage:  Option[AvatarImage]
)

