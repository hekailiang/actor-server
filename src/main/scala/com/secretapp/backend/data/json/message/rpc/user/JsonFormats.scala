package com.secretapp.backend.data.json.message.rpc.user

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.message.rpc.file._
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarChanged, RequestEditAvatar}
import com.secretapp.backend.data.json.message.struct._
import play.api.libs.json.Json

trait JsonFormats {

  implicit val requestSetAvatarFormat = Json.format[RequestEditAvatar]

  implicit val responseAvatarChanged = Json.format[ResponseAvatarChanged]

}
