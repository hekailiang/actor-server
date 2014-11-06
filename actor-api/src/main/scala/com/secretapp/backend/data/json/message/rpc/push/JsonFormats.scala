package com.secretapp.backend.data.json.message.rpc.push

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.rpc.push._
import play.api.libs.json.Json

trait JsonFormats {

  implicit val requestRegisterGooglePushFormat = Json.format[RequestRegisterGooglePush]
  implicit val requestUnregisterPushFormat     = UnitFormat[RequestUnregisterPush]

}
