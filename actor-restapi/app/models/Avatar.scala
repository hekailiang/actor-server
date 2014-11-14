package models

import models.CommonJsonFormats._
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Avatar(
  smallImage: Option[AvatarImage],
  largeImage: Option[AvatarImage],
  fullImage:  Option[AvatarImage]
)

object Avatar {

  implicit val jsonFormat: Format[Avatar] = (
    (JsPath \ "smallImage").format[Option[AvatarImage]] ~
    (JsPath \ "largeImage").format[Option[AvatarImage]] ~
    (JsPath \ "fullImage" ).format[Option[AvatarImage]]
  )(Avatar.apply, unlift(Avatar.unapply))

}
