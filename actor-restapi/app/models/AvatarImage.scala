package models

import models.CommonJsonFormats._
import play.api.libs.json.Json
import com.secretapp.backend.models.FileLocation
import models.json._

case class AvatarImage(fileLocation: FileLocation, width: Int, height: Int, fileSize: Int)

object AvatarImage {

  implicit val jsonFormat = Json.format[AvatarImage]

}
