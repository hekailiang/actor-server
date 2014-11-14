package models

import models.CommonJsonFormats._
import play.api.libs.json.Json

case class Error(message: String)

object Error {

  implicit val jsonFormat = Json.format[Error]

}
