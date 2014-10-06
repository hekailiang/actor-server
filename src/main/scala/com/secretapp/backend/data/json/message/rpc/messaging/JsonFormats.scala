package com.secretapp.backend.data.json.message.rpc.messaging

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.messaging._
import play.api.libs.json._

trait JsonFormats {
  implicit val encryptedKeyFormat     = Json.format[EncryptedKey]
  implicit val encryptedMessageFormat = Json.format[EncryptedMessage]
}
