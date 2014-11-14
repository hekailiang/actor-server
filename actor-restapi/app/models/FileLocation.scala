package models

import models.CommonJsonFormats._
import play.api.libs.json.Json

case class FileLocation(fileId: Long, accessHash: Long)

object FileLocation {

  implicit val jsonFormat = Json.format[FileLocation]

}
