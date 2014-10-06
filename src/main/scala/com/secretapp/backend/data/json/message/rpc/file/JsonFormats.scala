package com.secretapp.backend.data.json.message.rpc.file

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.file._
import play.api.libs.json._

trait JsonFormats {
  implicit val fileLocationFormat     = Json.format[FileLocation]
  implicit val uploadConfigFormat     = Json.format[UploadConfig]
}
