package models

import models.CommonJsonFormats._
import com.secretapp.backend.models.{AvatarImage, FileLocation}
import play.api.libs.json._
import play.api.libs.functional.syntax._

package object json {

  implicit val authSmsCodeJsonFormat = AuthSmsCode
  implicit val fileLocationJsonFormat = Json.format[FileLocation]
  implicit val avatarImageJsonFormat = Json.format[AvatarImage]
  implicit val avatarJsonFormat: Format[Avatar] = (
    (JsPath \ "smallImage").format[Option[AvatarImage]] ~
    (JsPath \ "largeImage").format[Option[AvatarImage]] ~
    (JsPath \ "fullImage" ).format[Option[AvatarImage]]
  )(Avatar.apply, unlift(Avatar.unapply))
}
