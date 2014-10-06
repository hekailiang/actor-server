package com.secretapp.backend.data.json

import play.api.libs.json._

case class MessageWithHeader(header: Int, body: JsObject)

object MessageWithHeader {
  implicit val format = Json.format[MessageWithHeader]
}
