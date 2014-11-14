package models

import models.CommonJsonFormats._
import play.api.libs.json.Json

case class AuthSmsCodeCreationRequest(smsHash: String, smsCode: String) {

  def toAuthSmsCode(phone: Long): models.AuthSmsCode =
    models.AuthSmsCode(phone, smsHash, smsCode)

}

object AuthSmsCodeCreationRequest {

  implicit val jsonFormat = Json.format[AuthSmsCodeCreationRequest]

}
