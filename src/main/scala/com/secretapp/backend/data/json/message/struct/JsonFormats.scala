package com.secretapp.backend.data.json.message.struct

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.message.rpc.file._
import com.secretapp.backend.data.json.types._
import com.secretapp.backend.data.message.struct._
import play.api.libs.json._

trait JsonFormats {
  case class Foo(id: Int, appId: Int, appTitle: String, deviceTitle: String, authTime: Long,
    authLocation: String, latitude: Option[Double], longitude: Option[Double])

  implicit val userIdFormat           = Json.format[UserId]
  implicit val userKeyFormat          = Json.format[UserKey]
  implicit val avatarImageFormat      = Json.format[AvatarImage]
  implicit val avatarFormat           = Json.format[Avatar]
  implicit val userFormat             = Json.format[User]
  implicit val authItemFormat         = Json.format[AuthItem]
}
