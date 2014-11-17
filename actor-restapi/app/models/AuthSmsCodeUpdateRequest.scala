package models

import models.CommonJsonFormats._
import play.api.libs.json.Json
import utils.OptSet._
import com.secretapp.backend.models

case class AuthSmsCodeUpdateRequest(
  smsHash: Option[String],
  smsCode: Option[String]
) {

  def update(c: models.AuthSmsCode): models.AuthSmsCode =
    c
      .optSet(smsHash)((c, v) => c.copy(smsHash = v))
      .optSet(smsCode)((c, v) => c.copy(smsCode = v))

}

object AuthSmsCodeUpdateRequest {

  implicit val jsonFormat = Json.format[AuthSmsCodeUpdateRequest]

}
