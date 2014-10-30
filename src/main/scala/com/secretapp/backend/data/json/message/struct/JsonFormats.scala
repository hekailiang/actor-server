package com.secretapp.backend.data.json.message.struct

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.message.rpc.file._
import com.secretapp.backend.data.message.struct._
import play.api.libs.json._

trait JsonFormats {
  case class Foo(id: Int, appId: Int, appTitle: String, deviceTitle: String, authTime: Long,
    authLocation: String, latitude: Option[Double], longitude: Option[Double])

  implicit val userIdFormat           = Json.format[UserId]
  implicit val userKeyFormat          = Json.format[UserKey]

  val userFormat: Format[User] = Json.format[User]
  implicit val implicitUserFormat = userFormat

  val authItemFormat: Format[AuthItem] = Json.format[AuthItem]
  implicit val implicitAuthItemFormat = authItemFormat
}
