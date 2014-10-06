package com.secretapp.backend.data.json.message.rpc.auth

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.message.rpc.auth._
import play.api.libs.json.Json

trait JsonFormats {
  implicit val requestAuthCodeFormat = Json.format[RequestAuthCode]
  implicit val requestSignInFormat = Json.format[RequestSignIn]
  implicit val requestSignUpFormat = Json.format[RequestSignUp]
}
