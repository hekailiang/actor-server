package models

import models.CommonJsonFormats._
import play.api.libs.json.Json
import com.secretapp.backend.models.FileLocation

package object json {

  implicit val authSmsCodeJsonFormat = AuthSmsCode
  implicit val fileLocationJsonFormat = Json.format[FileLocation]
}
