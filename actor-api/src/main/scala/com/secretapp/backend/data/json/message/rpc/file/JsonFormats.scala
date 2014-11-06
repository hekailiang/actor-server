package com.secretapp.backend.data.json.message.rpc.file

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.rpc.file._
import play.api.libs.json._

trait JsonFormats {
  val uploadConfigFormat: Format[UploadConfig] = Json.format[UploadConfig]
  implicit val implicitUploadConfigFormat = uploadConfigFormat

  implicit val requestCompleteUploadFormat = Json.format[RequestCompleteUpload]
  implicit val requestGetFileFormat = Json.format[RequestGetFile]
  implicit val requestStartUploadFormat = UnitFormat[RequestStartUpload]
  implicit val requestUploadPartFormat = Json.format[RequestUploadPart]

  implicit val responseFilePartFormat = Json.format[ResponseFilePart]
  implicit val responseUploadCompletedFormat = Json.format[ResponseUploadCompleted]
  implicit val responseUploadStartedFormat = Json.format[ResponseUploadStarted]
}
