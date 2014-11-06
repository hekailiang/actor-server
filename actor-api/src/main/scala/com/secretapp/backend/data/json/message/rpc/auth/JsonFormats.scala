package com.secretapp.backend.data.json.message.rpc.auth

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.UnitFormat
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.json.message.struct._
import play.api.libs.json.Json

trait JsonFormats {

  implicit val requestAuthCodeFormat = Json.format[RequestAuthCode]
  implicit val requestSignInFormat = Json.format[RequestSignIn]
  implicit val requestSignUpFormat = Json.format[RequestSignUp]

  implicit val requestGetAuthFormat = UnitFormat[RequestGetAuth]
  implicit val requestLogoutFormat = UnitFormat[RequestLogout]
  implicit val requestRemoveAuthFormat = Json.format[RequestRemoveAuth]
  implicit val requestRemoveAllOtherAuthsFormat = UnitFormat[RequestRemoveAllOtherAuths]

  implicit val responseAuthFormat = Json.format[ResponseAuth]
  implicit val responseGetAuthFormat = Json.format[ResponseGetAuth]
  implicit val responseAuthCodeFormat = Json.format[ResponseAuthCode]
}
